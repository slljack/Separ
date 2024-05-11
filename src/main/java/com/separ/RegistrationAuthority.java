package com.separ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.separ.config.ConfigData;
import com.separ.data.MessageInterface;
import com.separ.data.config.ConfigMessage;
import com.separ.data.config.ConstraintMessage;
import com.separ.data.config.CryptoMessage;
import com.separ.data.config.EntityInfoMessage;
import com.separ.data.config.TokenMessage;
import com.separ.entity.EntityInfo;
import com.separ.entity.EntityManager;
import com.separ.entity.EntityType;
import com.separ.flow.Pacemaker;
import com.separ.flow.StateMachineRunner;
import com.separ.flow.StateTracker;
import com.separ.flow.StateTracker.StateEvent;
import com.separ.token.Constraint;
import com.separ.token.Token;
import com.separ.utils.Benchmarker;
import com.separ.utils.ChannelManager;
import com.separ.utils.NetUtils;
import com.separ.utils.Printer;
import com.separ.utils.RACryptoUtils;
import com.separ.utils.UrlHelper;

import it.unibs.gs.interfaces.GSPublicKey;

public class RegistrationAuthority extends StateMachineRunner {

    Pacemaker pacemaker;
    protected ChannelManager channelManager;
    private List<Constraint> constraints;

    // Constraint ID -> Tokens
    private Map<Integer, List<Token>> tokens;

    Printer printer;
    Benchmarker benchmarker;

    RegistrationAuthority() {
        super();
        pacemaker = new Pacemaker();
        channelManager = new ChannelManager();
        constraints = new ArrayList<Constraint>();
        tokens = new HashMap<Integer, List<Token>>();

        StateTracker.addStateListener(this::stateChanged);
        ConfigData.loadConfig("ra");
        ConfigData.loadConfig("constraints");
        ConfigData.loadConfig("platform");
        ConfigData.loadConfig("experiment");

        printer = new Printer(null);
        benchmarker = new Benchmarker("RA", 0);

        var allowedModes = List.of("tokenOnly", "crashOnly", "byzantine");
        if (!allowedModes.contains(ConfigData.getString("experiment.mode"))) {
            printer.error("Invalid experiment mode. Allowed modes: \"tokenOnly\", \"crashOnly\", \"byzantine\"");
            return;
        }
    }

    @Override
    protected Map<String, Runnable> getStateMachine() {
        return Map.of("init", this::init, "config", this::config, "connect", this::connect, "execute", this::execute,
                "stop", this::stop);
    }

    public void init() {

        initConstraints();

        if (ConfigData.getString("experiment.mode").equals("tokenOnly")) {

            var entities = new HashMap<EntityType, Map<Integer, EntityInfo>>();

            for (var entityType : EntityType.values()) {
                entities.put(entityType, new HashMap<Integer, EntityInfo>());

                var count = ConfigData.getInt("experiment.entities." + entityType.id());
                for (var i = 0; i < count; i += 1) {
                    var info = new EntityInfo(entityType, i, null);
                    entities.get(entityType).put(i, info);
                }
            }

            generateTokens(entities);

            setState("stop");

            printer.info("Stopping.");
            channelManager.shutdown();

            benchmarker.report();
            return;
        }

        printer.info("Starting init.");

        try {
            var port = ConfigData.getInt("init.RegistrationAuthorityPort");
            channelManager.addInboundChannel("node", NetUtils.nettyGetInbound(this::readInbound), port);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        waiting = true;
        pacemaker.run();
    }

    void initConstraints() {
        var items = ConfigData.get("constraints.items").elements();
        while (items.hasNext()) {
            var item = items.next();
            var threshold = item.get("threshold").asInt();

            var targets = new ArrayList<EntityType>();
            var targetItems = item.get("targets").elements();
            while (targetItems.hasNext()) {
                var targetItem = targetItems.next().asText();
                targets.add(EntityType.get(targetItem));
            }

            constraints.add(Constraint.generate(targets, threshold));
        }

    }

    void generateTokens(Map<EntityType, Map<Integer, EntityInfo>> entities) {
        benchmarker.start("token-gen");
        printer.title("Constraints");

        var threadCount = ConfigData.getInt("experiment.tokenThreads");
        ExecutorService taskExecutor = Executors.newFixedThreadPool(threadCount);
        printer.title("Tokens");

        var futures = new HashMap<Integer, List<Future<Token>>>();

        for (var constraint : constraints) {
            printer.info(constraint);
            futures.put(constraint.getId(), constraint.generateTokens(entities, taskExecutor));
        }

        var nextTokenId = 0;
        try {
            for (var constraintId : futures.keySet()) {
                var tokenList = new ArrayList<Token>();
                for (var task : futures.get(constraintId)) {
                    var token = task.get();
                    token.setId(nextTokenId);
                    tokenList.add(token);
                    nextTokenId += 1;
                }

                tokens.put(constraintId, tokenList);
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        printer.info("Total: " + nextTokenId);

        taskExecutor.shutdownNow();
        benchmarker.end("token-gen");
        printer.info(Printer.tokensToString(tokens));
    }

    void config() {
        printer.info("\nStarting config.");

        printer.title("Entities");
        printer.info(Printer.entityMapToString(EntityManager.getEntityMap()));

        var entities = EntityManager.getAll();
        for (var entity : entities) {
            pacemaker.call(entity.type(), entity.getId(), "config");
        }

        var allChannel = channelManager.getOutboundGroup("all");
        var configMessage = new ConfigMessage(ConfigData.all());
        NetUtils.write(allChannel, configMessage);

        var entityMessage = new EntityInfoMessage(entities);
        NetUtils.write(allChannel, entityMessage);

        var constraintMessage = new ConstraintMessage(constraints);
        NetUtils.write(allChannel, constraintMessage);

        generateTokens(EntityManager.getEntityMap());

        if (ConfigData.getString("experiment.mode").equals("tokenOnly")) {
            setState("stop");

            printer.info("Stopping.");
            for (var entity : entities) {
                pacemaker.call(entity.type(), entity.getId(), "stop");
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            pacemaker.stop();
            channelManager.shutdown();

            benchmarker.report();
            return;
        }

        for (var entity : entities) {
            var entityTokens = new HashMap<Integer, List<Token>>();
            for (var constraint : constraints) {
                if (constraint.applies(entity)) {
                    var cid = constraint.getId();
                    var ectokens = constraint.getTokens(tokens.get(cid), entity);
                    entityTokens.put(cid, ectokens);
                }
            }

            var entityChannel = channelManager.getOutboundChannel(entity.type().id() + "-" + entity.getId());
            var tokenMessage = new TokenMessage(entityTokens);
            NetUtils.write(entityChannel, tokenMessage);
        }

        printer.info("Generating Group Certificates.\n");
        benchmarker.start("group-certificates");
        var allGPKs = new HashMap<EntityType, GSPublicKey>();
        for (var group : EntityType.values()) {
            RACryptoUtils.generateGroupCertificate(group);
            allGPKs.put(group, RACryptoUtils.getGroupCertificate(group).getGPK());
        }

        for (var entity : entities) {
            RACryptoUtils.generateMemberCertificate(entity.getId(), entity.type());
        }

        benchmarker.end("group-certificates");

        for (var entity : entities) {
            var myGPK = RACryptoUtils.getGroupCertificate(entity.type()).getGPK();
            var memberCertificate = RACryptoUtils.getMemberCertificates().get(entity.type()).get(entity.getId());
            var raPublicKey = RACryptoUtils.getSignaturePublicKey();

            var cryptoMessage = new CryptoMessage(allGPKs, myGPK, memberCertificate, raPublicKey);
            var entityChannel = channelManager.getOutboundChannel(entity.type().id() + "-" + entity.getId());
            NetUtils.write(entityChannel, cryptoMessage);
        }

        waiting = true;
    }

    void connect() {
        printer.info("Starting connect.");
        var entities = EntityManager.getAll();
        for (var entity : entities) {
            pacemaker.call(entity.type(), entity.getId(), "connect");
        }

        waiting = true;
    }

    void execute() {
        printer.info("Starting execute.");
        benchmarker.start("execute-duration");
        var entities = EntityManager.getAll();
        for (var entity : entities) {
            pacemaker.call(entity.type(), entity.getId(), "execute");
        }

        waiting = true;

    }

    void stop() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        printer.info("Stopping.");
        benchmarker.end("execute-duration");
        var entities = EntityManager.getAll();
        for (var entity : entities) {
            pacemaker.call(entity.type(), entity.getId(), "stop");
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        pacemaker.stop();
        channelManager.shutdown();

        benchmarker.report();

    }

    void stateChanged(StateEvent event) {
        if (event.target == null) {
            waiting = false;
            return;
        }

        var target = event.target;
        EntityManager.put(target, event.newState);
        if (event.newState.equals("init")) {
            var targetType = target.type();
            var targetId = target.getId();
            var port = UrlHelper.getEntityPort(targetType, targetId);
            var channelId = targetType.id() + "-" + targetId;
            channelManager.addOutboundChannel(targetType.id(), channelId, target.getIp(), port, NetUtils.nettyGet());
        }

        if (event.newState.equals("init")) {
            for (var entityType : EntityType.values()) {
                if (EntityManager.countState(entityType, "init") < ConfigData
                        .getInt("experiment.entities." + entityType.id())) {
                    return;
                }
            }

            setState("config");
        } else if (event.newState.equals("config")) {
            for (var entityType : EntityType.values()) {
                if (EntityManager.countState(entityType, "config") < ConfigData
                        .getInt("experiment.entities." + entityType.id())) {
                    return;
                }
            }

            setState("connect");
        } else if (event.newState.equals("connect")) {
            for (var entityType : EntityType.values()) {
                if (EntityManager.countState(entityType, "connect") < ConfigData
                        .getInt("experiment.entities." + entityType.id())) {
                    return;
                }
            }

            setState("execute");
        } else if (event.newState.equals("execute")) {
            if (EntityManager.countState(EntityType.REQUESTER, "execute") < ConfigData
                    .getInt("experiment.entities." + EntityType.REQUESTER.id())) {
                return;
            }

            setState("stop");
        }
    }

    @Override
    public void setState(String state) {
        super.setState(state);
    }

    void readInbound(MessageInterface message) {

    }

    public static void main(String[] args) {
        new RegistrationAuthority().run();
    }
}
