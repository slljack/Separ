package com.separ.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.separ.config.ConfigData;
import com.separ.data.entity.ControlMessage;
import com.separ.data.entity.ControlMessage.ControlMessageType;
import com.separ.data.entity.EntityMessageBase;
import com.separ.data.entity.EntityTransactionMessage;
import com.separ.data.entity.NodeTransactionMessage;
import com.separ.data.entity.NodeTransactionMessage.NodeMessageType;
import com.separ.token.Token;
import com.separ.transaction.RewardTransaction;
import com.separ.transaction.Transaction;
import com.separ.transaction.TransactionType;
import com.separ.utils.CommonUtils;
import com.separ.utils.NetUtils;

public class Platform extends EntityBase {

    private int nodeCount;
    private int platformCount;

    // Worker ID -> List of Assigned Platforms
    Map<Integer, List<Integer>> workersShare;

    // Message ID -> List of Worker IDs
    Map<MessageKey, List<Integer>> acceptedContribs;

    // Message ID -> Contribution Number -> Worker ID
    Map<MessageKey, Map<Integer, Integer>> submittedContribs;

    // Message ID -> Number of Token Responses Received
    Map<MessageKey, Integer> responseCounter;
    // Message ID -> Number of Signature Responses Received
    Map<MessageKey, Integer> signatureCounter;

    // Message ID -> Constraint Id -> List of Tokens
    Map<MessageKey, Map<Integer, List<Token>>> transactionTokens;

    // Message ID -> Token ID -> ...
    Map<MessageKey, Map<Integer, List<EntityKey>>> tokenParticipants;
    Map<MessageKey, Map<Integer, Map<EntityType, byte[]>>> tokenSignatures;
    Map<MessageKey, Map<Integer, Map<EntityType, byte[]>>> tokenTaskSignatures;

    // Message ID -> Requester Local Message ID
    private Map<MessageKey, MessageKey> requestKeyMap;

    private int nextGlobalId = 0;

    private Random rand;

    public Platform(int id, int nodeCount, int platformCount) {
        super(EntityType.PLATFORM, id);
        this.nodeCount = nodeCount;
        this.platformCount = platformCount;
        acceptedContribs = new HashMap<MessageKey, List<Integer>>();
        submittedContribs = new HashMap<MessageKey, Map<Integer, Integer>>();
        responseCounter = new HashMap<MessageKey, Integer>();
        signatureCounter = new HashMap<MessageKey, Integer>();
        transactionTokens = new HashMap<MessageKey, Map<Integer, List<Token>>>();
        tokenParticipants = new HashMap<MessageKey, Map<Integer, List<EntityKey>>>();
        tokenSignatures = new HashMap<MessageKey, Map<Integer, Map<EntityType, byte[]>>>();
        tokenTaskSignatures = new HashMap<MessageKey, Map<Integer, Map<EntityType, byte[]>>>();
        requestKeyMap = new HashMap<MessageKey, MessageKey>();
        rand = new Random();
        // printer.setEnabled(false);
    }

    private int getPrimaryNode() {
        return id * (nodeCount / platformCount);
    }

    private int getPrimaryNode(int platformId) {
        return platformId * (nodeCount / platformCount);
    }

    public void initGlobalKey(MessageKey globalKey) {
        submittedContribs.put(globalKey, new HashMap<Integer, Integer>());
        acceptedContribs.put(globalKey, new ArrayList<Integer>());
        responseCounter.put(globalKey, 0);
        signatureCounter.put(globalKey, 0);
        transactionTokens.put(globalKey, new HashMap<Integer, List<Token>>());
        tokenParticipants.put(globalKey, new HashMap<Integer, List<EntityKey>>());
        tokenSignatures.put(globalKey, new HashMap<Integer, Map<EntityType, byte[]>>());
        tokenTaskSignatures.put(globalKey, new HashMap<Integer, Map<EntityType, byte[]>>());
        for (var constraint : constraints) {
            var cid = constraint.getId();
            transactionTokens.get(globalKey).put(cid, new ArrayList<Token>());
        }
    }

    public void handleToken(Transaction transaction, Token token, int constraintId, MessageKey globalKey) {
        tokenParticipants.get(globalKey).put(token.getId(), new ArrayList<EntityKey>());
        var constraint = constraints.get(constraintId);
        transactionTokens.get(globalKey).get(constraintId).add(token);
        tokenSignatures.get(globalKey).put(token.getId(), new HashMap<EntityType, byte[]>());
        tokenTaskSignatures.get(globalKey).put(token.getId(), new HashMap<EntityType, byte[]>());
        for (var type : EntityType.values()) {
            if (constraint.applies(type)) {
                var participant = transaction.getParticipant(type);
                tokenParticipants.get(globalKey).get(token.getId()).add(participant);
            }
        }
    }

    @Override
    MessageKey addToMessageLog(EntityMessageBase message) {
        var prevLocalKey = message.getLocalKey();
        super.addToMessageLog(message);

        var globalKey = message.getGlobalKey();

        if (globalKey == null) {
            globalKey = new MessageKey(nextGlobalId, id, EntityType.PLATFORM);
            message.setGlobalKey(globalKey);
            initGlobalKey(globalKey);
            nextGlobalId += 1;

            requestKeyMap.put(globalKey, prevLocalKey);
        }

        return message.getGlobalKey();
    }

    @Override
    public void processEntityMessage(EntityMessageBase message) {
        int requiredContribution = ConfigData.getInt("platform.requiredContribution");

        if (message instanceof EntityTransactionMessage) {
            var entityMessage = (EntityTransactionMessage) message;
            addToMessageLog(entityMessage);
            printer.in(entityMessage);

            if (entityMessage.transaction.getType() == TransactionType.SUBMISSION) {
                benchmarker.start("process", message);
                benchmarker.start("task-initialization", message);
            }

            var globalKey = message.getGlobalKey();

            var nodeMessage = NodeTransactionMessage.fromEntityMessage(entityMessage);
            nodeMessage.messageType = NodeMessageType.REQUEST;
            nodeMessage.senderId = getPrimaryNode();
            addToMessageLog(nodeMessage);

            var transaction = nodeMessage.transaction;

            if (transaction.getType() == TransactionType.CONTRIBUTION) {
                var workerId = transaction.getParticipantId(EntityType.WORKER);

                var nextNum = submittedContribs.get(globalKey).size();

                transaction.getGlobalKey().setNum(nextNum);
                nodeMessage.digest = transaction.getDigest(false);
                submittedContribs.get(globalKey).put(nextNum, workerId);
            }

            var channel = channelManager.getOutboundChannel("node-" + getPrimaryNode());
            NetUtils.write(channel, nodeMessage);
        } else if (message instanceof NodeTransactionMessage) {

            var nodeMessage = (NodeTransactionMessage) message;
            addToMessageLog(nodeMessage);
            printer.in(nodeMessage);

            var transaction = nodeMessage.transaction;
            var globalKey = nodeMessage.getGlobalKey();

            if (transaction.getType() == TransactionType.SUBMISSION) {
                var platforms = transaction.getPlatforms();
                var eligibleWorkers = new ArrayList<Integer>();
                var chosenWorkers = new HashSet<Integer>();

                for (var workerId : workersShare.keySet()) {
                    var workerPlatforms = workersShare.get(workerId);
                    if (!Collections.disjoint(workerPlatforms, platforms)) {
                        eligibleWorkers.add(workerId);
                    }
                }

                var entityMessage = EntityTransactionMessage.fromNodeMessage(nodeMessage);
                entityMessage.transaction.addParticipant(type, getId());
                addToMessageLog(entityMessage);

                if (eligibleWorkers.size() <= requiredContribution) {
                    chosenWorkers.addAll(eligibleWorkers);
                } else {
                    while (chosenWorkers.size() < requiredContribution) {
                        var lottery = rand.nextInt(eligibleWorkers.size());
                        chosenWorkers.add(eligibleWorkers.get(lottery));
                    }
                }

                for (var workerId : chosenWorkers) {
                    var channel = channelManager.getOutboundChannel("worker-" + workerId);
                    NetUtils.write(channel, entityMessage);
                }

                benchmarker.end("task-initialization", message);
                benchmarker.start("task-assertion", message);
            } else if (transaction.getType() == TransactionType.CONTRIBUTION) {

                var contributionCount = acceptedContribs.get(globalKey).size();
                if (contributionCount >= requiredContribution) {
                    // TODO: abort the contribution
                    return;
                }

                var contribNum = nodeMessage.transaction.getGlobalKey().getNum();
                var workerId = submittedContribs.get(globalKey).get(contribNum);
                acceptedContribs.get(globalKey).add(workerId);
                contributionCount += 1;

                if (contributionCount >= requiredContribution) {
                    var result = reserveTokens(transaction, globalKey);
                    if (result != Boolean.FALSE) {
                        var tokens = getPublicTokens(globalKey);
                        for (var cid : tokens.keySet()) {
                            var token = tokens.get(cid);
                            handleToken(transaction, token, cid, globalKey);
                        }

                        responseCounter.put(globalKey, responseCounter.get(globalKey) + 1);
                    }
                }

                var controlMessage = new ControlMessage();
                controlMessage.type = ControlMessageType.TOKENS_REQ;
                controlMessage.transaction = transaction.copy();
                controlMessage.setGlobalKey(globalKey);
                addToMessageLog(controlMessage);

                var channel = channelManager.getOutboundChannel("worker-" + workerId);
                NetUtils.write(channel, controlMessage);

                if (contributionCount >= requiredContribution) {
                    benchmarker.end("task-assertion", message);
                    benchmarker.start("task-verification", message);
                    var requesterId = transaction.getParticipantId(EntityType.REQUESTER);
                    var channel2 = channelManager.getOutboundChannel("requester-" + requesterId);
                    NetUtils.write(channel2, controlMessage);
                }

                printer.out(controlMessage);
            } else if (transaction.getType() == TransactionType.REWARD) {
                acceptedContribs.remove(globalKey);
                submittedContribs.remove(globalKey);

                var entityMessage = EntityTransactionMessage.fromNodeMessage(nodeMessage);
                addToMessageLog(entityMessage);
                printer.out(entityMessage);

                var minId = Collections.min(nodeMessage.transaction.getGlobalKey().getIdMap().keySet());
                if (minId == id) {
                    for (var entityId : entities.get(EntityType.REQUESTER).keySet()) {
                        var requesterMessage = new EntityTransactionMessage(entityMessage);
                        requesterMessage.setLocalKey(requestKeyMap.get(entityMessage.getGlobalKey()));
                        var channel = channelManager.getOutboundChannel("requester-" + entityId);
                        NetUtils.write(channel, requesterMessage);
                    }

                    for (var entityId : entities.get(EntityType.WORKER).keySet()) {
                        var channel = channelManager.getOutboundChannel("worker-" + entityId);
                        NetUtils.write(channel, entityMessage);
                    }
                }

                var rewardTransaction = (RewardTransaction) transaction;
                verifyTokenSignatures(rewardTransaction.getTokenSignatures());
                benchmarker.end("task-verification", message);
                benchmarker.end("process", message);
            }
        } else if (message instanceof ControlMessage) {
            var controlMessage = (ControlMessage) message;
            var entityKey = controlMessage.getLocalKey();
            addToMessageLog(controlMessage);
            printer.in(controlMessage);

            var transaction = controlMessage.transaction;
            var globalKey = controlMessage.getGlobalKey();
            var platformTokens = transactionTokens.get(globalKey);
            if (controlMessage.type == ControlMessageType.TOKENS_RES) {
                if (responseCounter.get(globalKey) >= requiredContribution + 2) {
                    return;
                }

                for (var cid : controlMessage.tokens.keySet()) {
                    var token = controlMessage.tokens.get(cid);
                    handleToken(transaction, token, cid, globalKey);
                }

                responseCounter.put(globalKey, responseCounter.get(globalKey) + 1);

                if (responseCounter.get(globalKey) >= requiredContribution + 2) {
                    var newMessage = new ControlMessage();
                    newMessage.type = ControlMessageType.SIGNATURE_REQ;
                    newMessage.transaction = controlMessage.transaction.copy();
                    newMessage.setGlobalKey(controlMessage.getGlobalKey());
                    addToMessageLog(newMessage);

                    var participantTokenMap = new HashMap<EntityKey, List<Token>>();

                    for (var tokenList : platformTokens.values()) {
                        for (var token : tokenList) {
                            var id = token.getId();
                            var participants = tokenParticipants.get(globalKey).get(id);
                            for (var participant : participants) {
                                if (!participantTokenMap.containsKey(participant)) {
                                    participantTokenMap.put(participant, new ArrayList<Token>());
                                }
                                participantTokenMap.get(participant).add(token);
                            }
                        }
                    }

                    var taskBytes = transaction.toByteArray();

                    var myTokens = participantTokenMap.get(getEntityKey());
                    for (var token : myTokens) {
                        var tokenBytes = token.getBytes(false);
                        tokenSignatures.get(globalKey).get(token.getId()).put(getType(), crypto.groupSign(tokenBytes));

                        var joinedBytes = CommonUtils.joinBytes(tokenBytes, taskBytes);
                        tokenTaskSignatures.get(globalKey).get(token.getId()).put(getType(),
                                crypto.groupSign(joinedBytes));
                    }
                    signatureCounter.put(globalKey, signatureCounter.get(globalKey) + 1);
                    participantTokenMap.remove(getEntityKey());

                    for (var participant : participantTokenMap.keySet()) {
                        var targetMessage = new ControlMessage(newMessage);
                        targetMessage.tokens = CommonUtils.toMap(participantTokenMap.get(participant));
                        var targetChannel = channelManager
                                .getOutboundChannel(participant.getEntityType().id() + "-" + participant.getEntityId());
                        NetUtils.write(targetChannel, targetMessage);
                        printer.out(targetMessage);
                    }
                }
            } else if (controlMessage.type == ControlMessageType.SIGNATURE_RES) {
                if (signatureCounter.get(globalKey) >= requiredContribution + 2) {
                    return;
                }

                var tokens = controlMessage.tokens;

                for (var token : tokens.values()) {
                    var tokenSignMap = tokenSignatures.get(globalKey).get(token.getId());
                    tokenSignMap.put(entityKey.getEntityType(), controlMessage.tokenSignatures.get(token.getId()));

                    var tokenTaskSignMap = tokenTaskSignatures.get(globalKey).get(token.getId());
                    tokenTaskSignMap.put(entityKey.getEntityType(), controlMessage.tokenSignatures.get(token.getId()));
                }

                signatureCounter.put(globalKey, signatureCounter.get(globalKey) + 1);

                if (signatureCounter.get(globalKey) >= requiredContribution + 2) {
                    var rewardTransaction = new RewardTransaction();
                    rewardTransaction.setGlobalKey(transaction.getGlobalKey().copy());
                    rewardTransaction.setType(TransactionType.REWARD);
                    rewardTransaction.getGlobalKey().setNum(0);
                    rewardTransaction.setParticipants(new ArrayList<EntityKey>(transaction.getParticipants()));
                    rewardTransaction.timestamp = System.currentTimeMillis();
                    rewardTransaction.platforms = List.copyOf(transaction.platforms);

                    for (var list : platformTokens.values()) {
                        for (var token : list) {
                            rewardTransaction.addToken(token);
                        }
                    }

                    rewardTransaction.setTokenSignatures(tokenSignatures.get(globalKey));
                    rewardTransaction.setTokenTaskSignatures(tokenTaskSignatures.get(globalKey));

                    var newMessage = new NodeTransactionMessage();
                    newMessage.setGlobalKey(globalKey);
                    newMessage.transaction = rewardTransaction.copy();
                    newMessage.senderId = getPrimaryNode();
                    newMessage.messageType = NodeMessageType.REQUEST;
                    newMessage.digest = rewardTransaction.getDigest(false);
                    addToMessageLog(newMessage);
                    printer.out(newMessage);

                    var platforms = rewardTransaction.getPlatforms();
                    for (var platform : platforms) {
                        var platformMessage = new NodeTransactionMessage(newMessage);
                        platformMessage.transaction.setPlatforms(List.of(platform));
                        var channel = channelManager.getOutboundChannel("node-" + getPrimaryNode(platform));
                        NetUtils.write(channel, platformMessage);
                    }
                }
            }
        }
    }

    private void initWorkersShare() {
        workersShare = new HashMap<Integer, List<Integer>>();

        for (var workerId : entities.get(EntityType.WORKER).keySet()) {
            var platformList = ConfigData.getIntList("experiment.workerPlatforms." + workerId);
            workersShare.put(workerId, platformList);
        }
    }

    @Override
    protected void connect() {
        initWorkersShare();
        super.connect();
    }

    @Override
    protected void execute() {
        waiting = true;
    }

    @Override
    boolean shouldConnect(EntityInfo entity) {
        return true;
    }

    public static void main(String[] args) {
        if (args.length < 3 || args.length > 4) {
            System.out.println("Wrong parameter count.");
            return;
        }

        int platformCount, nodeCount, platformStartId, platformEndId;

        try {
            platformCount = Integer.parseInt(args[0]);
            nodeCount = Integer.parseInt(args[1]);
            platformStartId = Integer.parseInt(args[2]);
            if (args.length == 3) {
                platformEndId = platformStartId;
            } else {
                platformEndId = Integer.parseInt(args[3]);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
            return;
        }

        for (var pid = platformStartId; pid <= platformEndId; pid++) {
            new Platform(pid, nodeCount, platformCount).start();
        }
    }
}
