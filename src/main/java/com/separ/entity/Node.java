package com.separ.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.separ.config.ConfigData;
import com.separ.data.MessageInterface;
import com.separ.data.entity.EntityMessageBase;
import com.separ.data.entity.NodeTransactionMessage;
import com.separ.data.entity.NodeTransactionMessage.NodeMessageType;
import com.separ.entity.NodeTransactionProcessor.ProcessorState;
import com.separ.flow.StateTracker.StateEvent;
import com.separ.transaction.Transaction;
import com.separ.transaction.TransactionType;
import com.separ.utils.ManagedQueue;
import com.separ.utils.MinimalSet;
import com.separ.utils.NetUtils;
import com.separ.utils.UrlHelper;

public class Node extends EntityBase {

    private int shardId;
    private int localOrder = 0;
    private boolean isRunning = true;
    NodeTransactionProcessor processor;
    private Map<MessageKey, Integer> assignedKeys;

    private MinimalSet issuedTransactionIds;

    Node(int id, int shardId) {
        super(EntityType.NODE, id);
        this.shardId = shardId;
        processor = new NodeTransactionProcessor(this);
        assignedKeys = new HashMap<MessageKey, Integer>();
        printer.setEnabled(false);
        issuedTransactionIds = new MinimalSet();
    }

    public int getShardId() {
        return shardId;
    }

    private int nodesPerShard() {
        var shardCount = entities.get(EntityType.PLATFORM).size();
        var nodeCount = entities.get(EntityType.NODE).size();
        return nodeCount / shardCount;
    }

    int getPrimaryNodeId(int shardId) {
        return shardId * nodesPerShard();
    }

    public int getSuperPrimary(NodeTransactionMessage message) {
        return getSuperPrimary(message.transaction);
    }

    public int getSuperPrimary(Transaction transaction) {
        return getPrimaryNodeId(Collections.min(transaction.getPlatforms()));
    }

    public boolean isPrimary() {
        return id % nodesPerShard() == 0;
    }

    public boolean isCrashOnly() {
        return ConfigData.getString("experiment.mode").equals("crashOnly");
    }

    public boolean isByzantine() {
        return ConfigData.getString("experiment.mode").equals("byzantine");
    }

    public int getShardId(int nodeId) {
        return nodeId / nodesPerShard();
    }

    public Map<MessageKey, Integer> getAssignedKeys() {
        return assignedKeys;
    }

    public MinimalSet getIssuedTransactionIds() {
        return issuedTransactionIds;
    }

    @Override
    public void init() {
        try {
            var port = UrlHelper.getEntityPort(type, id);
            channelManager.addInboundChannel("entity", NetUtils.nettyGetInbound(this::readInboundMessage), port);

            var port2 = UrlHelper.getEntityPort(type, id) + 2;
            channelManager.addInboundChannel("node", NetUtils.nettyGetInbound(this::readNodeInbound), port2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        waiting = true;

        pacekeeper.run();
        pacekeeper.call("init");
    }

    @Override
    public void config() {
        waiting = true;
    }

    @Override
    public void connect() {
        processor.init();
        processor.start();

        var shardCount = entities.get(EntityType.PLATFORM).size();
        var nodesPerShard = nodesPerShard();

        for (var shardId = 0; shardId < shardCount; shardId++) {
            for (var i = 0; i < nodesPerShard; i++) {
                var nodeId = shardId * nodesPerShard + i;
                if (nodeId == getId())
                    continue;

                var entity = entities.get(EntityType.NODE).get(nodeId);

                var ip = entity.getIp();
                var port = UrlHelper.getEntityPort(EntityType.NODE, nodeId) + 2;
                channelManager.addOutboundChannel("shard-" + shardId, "node-" + nodeId, ip, port, NetUtils.nettyGet());
            }
        }

        if (isPrimary()) {
            var entity = entities.get(EntityType.PLATFORM).get(shardId);

            var ip = entity.getIp();
            var port = UrlHelper.getEntityPort(EntityType.PLATFORM, shardId);
            channelManager.addOutboundChannel("platform", ip, port, NetUtils.nettyGet());
        }

        waiting = true;
        pacekeeper.call("connect");
    }

    private void callPrimaries(NodeTransactionMessage message) {
        var platforms = message.transaction.getPlatforms();
        for (var targetId : platforms) {
            if (targetId == shardId) {
                continue;
            }

            var primaryId = getPrimaryNodeId(targetId);
            var channel = channelManager.getOutboundChannel("node-" + primaryId);
            NetUtils.write(channel, message);
        }
    }

    private void callAll(NodeTransactionMessage message) {
        var platforms = message.transaction.getPlatforms();
        for (var targetId : platforms) {
            if (targetId == shardId) {
                continue;
            }

            var channel = channelManager.getOutboundGroup("shard-" + targetId);
            NetUtils.write(channel, message);
        }
    }

    @Override
    public void execute() {
        while (isRunning) {
            var message = processor.outputQueue.poll();
            if (message == null) {
                continue;
            }

            printer.out(message);

            if (isByzantine()) {
                sendByzantine(message);
            } else {
                sendCrashOnly(message);
            }
        }

        processor.doNotify();
        benchmarker.report();
    }

    private void sendByzantine(NodeTransactionMessage message) {
        var messageType = message.messageType;
        var transactionType = message.transaction.getType();
        var platforms = message.transaction.getPlatforms();

        if (messageType == NodeMessageType.COMMIT && message.sendToPlatform == true) {
            var channel = channelManager.getOutboundChannel("platform");
            NetUtils.write(channel, message);
        } else {
            if (transactionType == TransactionType.REWARD && messageType != NodeMessageType.PROPOSE) {
                var channel = channelManager.getOutboundGroup("shard-" + shardId);
                if (channel.size() > 3) {
                    printer.error("Group size larget than expected [shard-" + shardId + "] : " + channel.size());
                }
                NetUtils.write(channel, message);
            } else {
                for (var targetId : platforms) {
                    var channel = channelManager.getOutboundGroup("shard-" + targetId);
                    NetUtils.write(channel, message);
                }
            }
        }
    }

    private void sendCrashOnly(NodeTransactionMessage message) {
        var messageType = message.messageType;
        var transactionType = message.transaction.getType();
        var platforms = message.transaction.getPlatforms();

        if (platforms.size() > 1 && getId() == getSuperPrimary(message)) {
            if (messageType == NodeMessageType.PROPOSE) {
                callPrimaries(message);
            } else if (messageType == NodeMessageType.COMMIT) {
                callAll(message);
            }
        }

        if (isPrimary()) {
            if (messageType == NodeMessageType.COMMIT || messageType == NodeMessageType.PROPOSE) {
                var channel = channelManager.getOutboundGroup("shard-" + shardId);
                NetUtils.write(channel, message);
            }

            if (messageType == NodeMessageType.COMMIT) {
                var channel = channelManager.getOutboundChannel("platform");
                NetUtils.write(channel, message);
            }
        }

        if (messageType == NodeMessageType.ACCEPT) {
            int target;
            if (transactionType == TransactionType.REWARD) {
                target = getPrimaryNodeId(shardId);
            } else {
                target = getSuperPrimary(message);
            }

            var channel = channelManager.getOutboundChannel("node-" + target);
            NetUtils.write(channel, message);
        }
    }

    private void readNodeInbound(MessageInterface message) {
        if (message instanceof NodeTransactionMessage) {
            var tm = (NodeTransactionMessage) message;
//            printer.in(tm);

            sendToQueue(processor.inputQueue, tm, "Node input");
            processor.doNotify();
        }
    }

    @Override
    public void processEntityMessage(EntityMessageBase message) {
        if (message instanceof NodeTransactionMessage) {
            var tm = (NodeTransactionMessage) message;
//            printer.in(tm);

            sendToQueue(processor.requestQueue, tm, "Request input");

            if (processor.getNodeState() == ProcessorState.FREE) {
                processor.doNotify();
            }
        }
    }

    private void sendToQueue(ManagedQueue queue, NodeTransactionMessage message, String queueTitle) {
        message.setLocalOrder(localOrder);
        localOrder += 1;

        var transaction = message.transaction;
        var key = message.getGlobalKey();
        if (transaction.getType() == TransactionType.SUBMISSION) {
            if (transaction.getLocalKey(shardId) == null) {
                if (assignedKeys.containsKey(key)) {
                    transaction.setLocalId(shardId, assignedKeys.get(key));
//                    printer.info("tid has assigned: " + key + " : " + assignedKeys.get(key));
                } else {
                    var nextId = issuedTransactionIds.next();
                    transaction.setLocalId(shardId, nextId);
                    assignedKeys.put(key, nextId);
                    issuedTransactionIds.add(nextId);
//                    printer.info("tid issued new: " + key + " : " + assignedKeys.get(key));
                }
            } else {
                var localId = transaction.getLocalKey(shardId).getId();
                assignedKeys.put(key, localId);
                issuedTransactionIds.add(localId);
//                printer.info("tid received : " + key + " : " + assignedKeys.get(key));
            }
        }

        queue.offer(message);
    }

    @Override
    public void stop() {
        pacekeeper.stop();
        channelManager.shutdown();
    }

    @Override
    void stateChanged(StateEvent event) {
        super.stateChanged(event);
        if (event.newState.equals("stop")) {
            isRunning = false;
        }
    }

    @Override
    boolean shouldConnect(EntityInfo entity) {
        return entity.type() == EntityType.PLATFORM && entity.getId() == shardId;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Wrong parameter count.");
            return;
        }

        int platformId, nodeStartId, nodeEndId;

        try {
            platformId = Integer.parseInt(args[0]);
            nodeStartId = Integer.parseInt(args[1]);
            if (args.length == 2) {
                nodeEndId = nodeStartId;
            } else {
                nodeEndId = Integer.parseInt(args[2]);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
            return;
        }

        for (var nodeId = nodeStartId; nodeId <= nodeEndId; nodeId++) {
            new Node(nodeId, platformId).start();
        }
    }
}
