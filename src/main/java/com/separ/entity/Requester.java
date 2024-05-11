package com.separ.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.separ.config.ConfigData;
import com.separ.data.entity.ControlMessage;
import com.separ.data.entity.ControlMessage.ControlMessageType;
import com.separ.data.entity.EntityMessageBase;
import com.separ.data.entity.EntityTransactionMessage;
import com.separ.transaction.GlobalKey;
import com.separ.transaction.RewardTransaction;
import com.separ.transaction.Transaction;
import com.separ.transaction.TransactionType;
import com.separ.utils.CommonUtils;
import com.separ.utils.NetUtils;

public class Requester extends EntityBase {

    private Random rand;

    private int rewardCounter;

    public Requester(int id) {
        super(EntityType.REQUESTER, id);
        rand = new Random();
        rewardCounter = 0;
    }

    @Override
    public void processEntityMessage(EntityMessageBase message) {
        if (message instanceof EntityTransactionMessage) {
            if (message.transaction.getType() == TransactionType.REWARD) {
                var rewardTransaction = (RewardTransaction) message.transaction;
                verifyTokenSignatures(rewardTransaction.getTokenSignatures());

                rewardCounter += 1;
            }

            addToMessageLog(message);
            printer.in(message);

            var requestCount = ConfigData.getInt("experiment.requestCount");
            if (rewardCounter >= requestCount) {
                waiting = false;
                pacekeeper.call("execute");
            }
        } else if (message instanceof ControlMessage) {
            var controlMessage = (ControlMessage) message;
            addToMessageLog(controlMessage);
            printer.in(controlMessage);

            var transaction = controlMessage.transaction;
            var globalKey = controlMessage.getGlobalKey();
            if (controlMessage.type == ControlMessageType.TOKENS_REQ) {
                var result = reserveTokens(transaction, globalKey);
                if (result != Boolean.FALSE) {
                    var newMessage = new ControlMessage(controlMessage);
                    newMessage.type = ControlMessageType.TOKENS_RES;
                    addToMessageLog(newMessage);

                    var tokens = getPublicTokens(globalKey);
                    newMessage.tokens.putAll(tokens);

                    var targetPlatform = controlMessage.getGlobalKey().getEntityId();
                    var channel = channelManager.getOutboundChannel("platform-" + targetPlatform);
                    NetUtils.write(channel, newMessage);
                    printer.out(newMessage);
                }
            } else if (controlMessage.type == ControlMessageType.SIGNATURE_REQ) {
                var newMessage = new ControlMessage(controlMessage);
                newMessage.type = ControlMessageType.SIGNATURE_RES;
                addToMessageLog(newMessage);

                var tokens = controlMessage.tokens;
                for (var tokenId : tokens.keySet()) {
                    var token = tokens.get(tokenId);
                    var tokenBytes = token.getBytes(false);
                    newMessage.tokenSignatures.put(tokenId, crypto.groupSign(tokenBytes));

                    var taskBytes = controlMessage.transaction.toByteArray();
                    var joinedBytes = CommonUtils.joinBytes(tokenBytes, taskBytes);
                    newMessage.tokenTaskSignatures.put(tokenId, crypto.groupSign(joinedBytes));
                }

                var targetPlatform = controlMessage.getGlobalKey().getEntityId();
                var channel = channelManager.getOutboundChannel("platform-" + targetPlatform);
                NetUtils.write(channel, newMessage);

                printer.out(newMessage);
            }
        }
    }

    @Override
    protected void execute() {
        var requestInterval = ConfigData.getInt("experiment.requestIntervalMs");
        var requesterCount = ConfigData.getInt("experiment.entities.requester");
        var requestCount = ConfigData.getInt("experiment.requestCount") / requesterCount;

        var initialDelay = rand.nextInt(requestInterval);
        try {
            Thread.sleep(initialDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (var i = 0; i < requestCount; i++) {

            try {
                Thread.sleep(requestInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            var transaction = createTransaction();

            var requestMessage = new EntityTransactionMessage();
            requestMessage.timestamp = System.currentTimeMillis();
            requestMessage.transaction = transaction;
            addToMessageLog(requestMessage);
            printer.out(requestMessage);

            var superPrimary = Collections.min(transaction.getPlatforms());
            var channel = channelManager.getOutboundChannel("platform-" + superPrimary);
            NetUtils.write(channel, requestMessage);
        }

        waiting = true;
    }

    @Override
    void stop() {

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        super.stop();
    }

    @Override
    boolean shouldConnect(EntityInfo entity) {
        return entity.type() == EntityType.PLATFORM;
    }

    public Transaction createTransaction() {
        var transaction = new Transaction();
        transaction.setParticipants(new ArrayList<EntityKey>());
        transaction.addParticipant(EntityType.REQUESTER, getId());
        transaction.timestamp = System.currentTimeMillis();
        var key = new GlobalKey();
        key.setType(TransactionType.SUBMISSION);
        key.setNum(0);
        transaction.setGlobalKey(key);

        var shardCount = entities.get(EntityType.PLATFORM).size();
        var crossPlatformPercent = ConfigData.getInt("experiment.crossPlatformPercent");

        var roll = rand.nextInt(100);
        if (roll < crossPlatformPercent) {
            // 2 Platforms
            var targets = new ArrayList<Integer>();

            while (targets.size() < 2) {
                var target = rand.nextInt(shardCount);
                if (!targets.contains(target)) {
                    targets.add(target);
                }
            }
            transaction.setPlatforms(targets);
        } else {
            // 1 Platform
            var target = rand.nextInt(shardCount);
            transaction.setPlatforms(List.of(target));
        }

        return transaction;
    }

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Requester ID not given.");
            return;
        }

        int startId, endId;

        try {
            startId = Integer.parseInt(args[0]);
            endId = args.length == 1 ? startId : Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
            return;
        }

        for (var id = startId; id <= endId; id++) {
            new Requester(id).start();
        }
    }
}
