package com.separ.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.separ.config.ConfigData;
import com.separ.data.MessageInterface;
import com.separ.data.config.ConfigMessage;
import com.separ.data.config.ConstraintMessage;
import com.separ.data.config.CryptoMessage;
import com.separ.data.config.EntityInfoMessage;
import com.separ.data.config.TokenMessage;
import com.separ.data.entity.EntityMessageBase;
import com.separ.flow.Pacekeeper;
import com.separ.flow.StateMachineRunner;
import com.separ.flow.StateTracker;
import com.separ.flow.StateTracker.StateEvent;
import com.separ.token.Budget;
import com.separ.token.Constraint;
import com.separ.token.Token;
import com.separ.transaction.Transaction;
import com.separ.utils.Benchmarker;
import com.separ.utils.ChannelManager;
import com.separ.utils.CryptoUtils;
import com.separ.utils.NetUtils;
import com.separ.utils.Printer;
import com.separ.utils.UrlHelper;

public abstract class EntityBase extends StateMachineRunner implements EntityInterface {

    int id;
    EntityType type;

    ChannelManager channelManager;
    Pacekeeper pacekeeper;

    Map<EntityType, Map<Integer, EntityInfo>> entities;
    List<Constraint> constraints;

    Map<Integer, Token> allTokens;
    private Budget budget;

    // Local Key -> Message
    Map<MessageKey, EntityMessageBase> messageLog;

    // Local Key -> Global Key
    Map<MessageKey, MessageKey> localKeyMap;

    CryptoUtils crypto;

    private int nextMessageId = 0;
    int localOrder = 0;
    Printer printer;

    Benchmarker benchmarker;

    EntityBase(EntityType type, int id) {
        super();
        channelManager = new ChannelManager();
        StateTracker.addStateListener(this::stateChanged);
        this.type = type;
        this.id = id;

        entities = new HashMap<EntityType, Map<Integer, EntityInfo>>();
        for (var entityType : EntityType.values()) {
            entities.put(entityType, new HashMap<Integer, EntityInfo>());
        }

        allTokens = new HashMap<Integer, Token>();
        budget = new Budget();

        messageLog = new HashMap<MessageKey, EntityMessageBase>();
        localKeyMap = new HashMap<MessageKey, MessageKey>();

        printer = new Printer(type.symbol() + id);
        pacekeeper = new Pacekeeper(this);

        benchmarker = new Benchmarker(type.id() + " #" + id, 0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    public EntityKey getEntityKey() {
        return new EntityKey(getId(), getType());
    }

    @Override
    protected Map<String, Runnable> getStateMachine() {
        return Map.of("init", this::init, "config", this::config, "connect", this::connect, "execute", this::execute,
                "stop", this::stop);
    }

    public Printer getPrinter() {
        return printer;
    }

    @Override
    public void run() {
        var stateMachine = getStateMachine();
        String lastState = null;

        while (!getState().equals("stop")) {

            // Wait for a signal
            while (waiting) {
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (getState().equals("stop")) {
                break;
            }

            if (lastState != null) {
                benchmarker.end("state-" + lastState);
                lastState = null;
            }

            var method = stateMachine.get(getState());
            if (method != null) {
                lastState = getState();
                benchmarker.start("state-" + lastState);
                method.run();
            }
        }

        if (lastState != null) {
            benchmarker.end("state-" + lastState);
        }

        stop();
    }

    void init() {

        try {
            var port = UrlHelper.getEntityPort(type, id);
            channelManager.addInboundChannel("entity", NetUtils.nettyGetInbound(this::readInboundMessage), port);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        waiting = true;

        pacekeeper.run();
        pacekeeper.call("init");
    }

    void config() {
        waiting = true;
    }

    void connect() {

        for (var group : entities.keySet()) {
            for (var entity : entities.get(group).values()) {
                if (!shouldConnect(entity)) {
                    continue;
                }

                var entityType = entity.type();
                var entityId = entity.getId();
                var port = UrlHelper.getEntityPort(entityType, entityId);
                var channelId = entityType.id() + "-" + entityId;
                channelManager.addOutboundChannel(entityType.id(), channelId, entity.getIp(), port,
                        NetUtils.nettyGet());
            }
        }

        waiting = true;
        pacekeeper.call("connect");
    }

    abstract boolean shouldConnect(EntityInfo entity);

    void readInboundMessage(MessageInterface message) {
        if (getState().equals("stop")) {
            return;
        }

        if (message instanceof ConfigMessage) {
            var cm = (ConfigMessage) message;
            var configMap = cm.getConfigMap();
            ConfigData.putAll(configMap);
        } else if (message instanceof EntityInfoMessage) {
            var em = (EntityInfoMessage) message;
            for (var entity : em.getEntities()) {
                entities.get(entity.type()).put(entity.getId(), entity);
            }

            if (type != EntityType.NODE) {
                printer.title("Entities");
                printer.info(Printer.entityMapToString(entities));
            }
        } else if (message instanceof ConstraintMessage) {
            var cm = (ConstraintMessage) message;
            constraints = cm.getConstraints();

            if (type != EntityType.NODE) {
                printer.title("Constraints");
                for (var constraint : constraints) {
                    printer.info(constraint);
                }
            }
        } else if (message instanceof TokenMessage) {
            var tm = (TokenMessage) message;
            var messageTokens = tm.getTokens();

            for (var constraintId : messageTokens.keySet()) {
                var constraintTokens = messageTokens.get(constraintId);
                for (var token : constraintTokens) {
                    allTokens.put(token.getId(), token);
                    if (token.getPrivateKey() != null) {
                        budget.addToken(constraintId, token);
                    }
                }
            }

            if (type != EntityType.NODE) {
                printer.title("Tokens");
                printer.info(Printer.tokensToString(messageTokens));
            }
        } else if (message instanceof CryptoMessage) {
            var rm = (CryptoMessage) message;
            crypto = new CryptoUtils(rm.getAllGPKs(), rm.getMyGPK(), rm.getMemberCertificate(), rm.getRaPublicKey());

            waiting = false;
            pacekeeper.call("config");
        } else if (message instanceof EntityMessageBase) {
            var entityMessage = (EntityMessageBase) message;
            processEntityMessage(entityMessage);
        }
    }

    MessageKey addToMessageLog(EntityMessageBase message) {
        var localKey = new MessageKey(nextMessageId, id, type);
        message.setLocalKey(localKey);
        messageLog.put(localKey, message);
        localKeyMap.put(localKey, message.getGlobalKey());
        nextMessageId += 1;

        return localKey;
    }

    Boolean reserveTokens(Transaction transaction, MessageKey globalKey) {
        var isInitiator = false;
        var hasToken = true;
        for (var constraint : constraints) {
            if (constraint.applies(type) && constraint.isInitiator(entities.get(type).get(id))) {
                isInitiator = true;
                if (!budget.checkTokens(constraint.getId(), transaction, globalKey)) {
                    hasToken = false;
                    break;
                }
            }
        }

        if (isInitiator == false) {
            return null;
        }

        return hasToken;
    }

    Map<Integer, Token> getPublicTokens(MessageKey globalKey) {
        var map = new HashMap<Integer, Token>();

        var tokens = budget.spendTokens(globalKey);
        if (tokens != null) {
            for (var key : tokens.keySet()) {
                var token = tokens.get(key);
                var publicToken = new Token(token);
                publicToken.setPrivateKey(null);
                map.put(key, publicToken);
            }
        }

        return map;
    }

    boolean verifyTokenSignatures(Map<Integer, Map<EntityType, byte[]>> signatureMap) {

        for (var tokenId : signatureMap.keySet()) {
            var tokenSignatures = signatureMap.get(tokenId);

            for (var group : tokenSignatures.keySet()) {
                var signature = tokenSignatures.get(group);
                var token = allTokens.get(tokenId);
                if (token != null) {
                    if (crypto.groupVerify(group, token.getBytes(false), signature)) {
                        printer.info("Verified token #" + tokenId + " signature. (" + group.id() + ")");
                    } else {
                        printer.error("Failed token #" + tokenId + " signature. (" + group.id() + ")");
                    }
                }
            }
        }

        return true;
    }

    protected abstract void execute();

    void stop() {
        pacekeeper.stop();
        channelManager.shutdown();
        benchmarker.report();
    }

    void stateChanged(StateEvent event) {
        if (event.target == null) {
            waiting = false;
        }
    }
}
