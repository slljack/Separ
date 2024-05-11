package com.separ.entity;

import java.util.Collections;

import com.separ.data.entity.ControlMessage;
import com.separ.data.entity.ControlMessage.ControlMessageType;
import com.separ.data.entity.EntityMessageBase;
import com.separ.data.entity.EntityTransactionMessage;
import com.separ.transaction.RewardTransaction;
import com.separ.transaction.TransactionType;
import com.separ.utils.CommonUtils;
import com.separ.utils.NetUtils;

public class Worker extends EntityBase {

    public Worker(int id) {
        super(EntityType.WORKER, id);
    }

    int rewardCount = 0;

    @Override
    public void processEntityMessage(EntityMessageBase message) {
        if (message instanceof EntityTransactionMessage) {
            var entityMessage = (EntityTransactionMessage) message;
            addToMessageLog(entityMessage);
            printer.in(entityMessage);

            var transaction = entityMessage.transaction;

            if (transaction.getType() == TransactionType.SUBMISSION) {
                benchmarker.start("process", message);
                var newTransaction = transaction.copy();
                newTransaction.setType(TransactionType.CONTRIBUTION);
                newTransaction.addParticipant(getType(), getId());
                var result = reserveTokens(newTransaction, entityMessage.getGlobalKey());
                if (result == Boolean.FALSE) {
                    return;
                }

                var newMessage = EntityTransactionMessage.fromEntityMessage(entityMessage, newTransaction);
                addToMessageLog(entityMessage);

                var targetPlatform = Collections.min(transaction.getPlatforms());
                var channel = channelManager.getOutboundChannel("platform-" + targetPlatform);
                NetUtils.write(channel, newMessage);

                printer.out(newMessage);
            } else if (transaction.getType() == TransactionType.REWARD) {
                var rewardTransaction = (RewardTransaction) transaction;
                benchmarker.start("verify", message);
                verifyTokenSignatures(rewardTransaction.getTokenSignatures());
                rewardCount++;
                printer.info("REWARD COUNT: " + rewardCount);
                benchmarker.end("verify", message);
                benchmarker.end("process", message);
            }
        } else if (message instanceof ControlMessage) {
            var controlMessage = (ControlMessage) message;
            addToMessageLog(controlMessage);

            var newMessage = new ControlMessage(controlMessage);
            addToMessageLog(newMessage);

            if (controlMessage.type == ControlMessageType.TOKENS_REQ) {
                newMessage.type = ControlMessageType.TOKENS_RES;

                var tokens = getPublicTokens(controlMessage.getGlobalKey());
                newMessage.tokens.putAll(tokens);

                var targetPlatform = controlMessage.getGlobalKey().getEntityId();
                var channel = channelManager.getOutboundChannel("platform-" + targetPlatform);
                NetUtils.write(channel, newMessage);

                printer.out(newMessage);
            } else if (controlMessage.type == ControlMessageType.SIGNATURE_REQ) {
                newMessage.type = ControlMessageType.SIGNATURE_RES;
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
        waiting = true;
    }

    @Override
    boolean shouldConnect(EntityInfo entity) {
        return entity.type() == EntityType.PLATFORM;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Worker ID not given.");
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
            new Worker(id).start();
        }
    }
}
