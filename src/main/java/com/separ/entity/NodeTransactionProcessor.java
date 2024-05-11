package com.separ.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import com.separ.config.ConfigData;
import com.separ.data.entity.NodeTransactionMessage;
import com.separ.data.entity.NodeTransactionMessage.NodeMessageType;
import com.separ.transaction.GlobalKey;
import com.separ.transaction.LocalKey;
import com.separ.transaction.Transaction;
import com.separ.transaction.TransactionType;
import com.separ.utils.ManagedQueue;
import com.separ.utils.MinimalSet;
import com.separ.utils.NodeTally;
import com.separ.utils.Printer;

class NodeTransactionProcessor implements Runnable {

    ManagedQueue inputQueue;
    ManagedQueue outputQueue;
    ManagedQueue requestQueue;
    private ArrayList<NodeTransactionMessage> retainerList;
    private int myShardId;

    /* Node Level */

    private NodeTally acceptTally;
    private NodeTally commitTally;
    private TimeKeeper timekeeper;

    private LinkedList<LocalKey> transactionLog;

    // Local Transaction Key -> Transaction>
    private HashMap<LocalKey, Transaction> transactionLogValues;

    // Shard ID -> Set of Local Transaction Keys
    private Map<Integer, HashSet<LocalKey>> committedLog;

    private Transaction nextTransaction;

    private Map<LocalKey, NodeTransactionMessage> requests;

    /* Platform Level */

    // Platform ID -> Set of Transaction IDs
    private Map<Integer, HashSet<Integer>> rewardedLog;

    // Platform ID -> Transaction Type, Sorted Set of Transaction IDs
    private Map<Integer, Map<TransactionType, MinimalSet>> completedSets;

    // Platform ID -> Transaction Type -> Set of Transaction IDs
    private Map<Integer, Map<TransactionType, HashSet<Integer>>> activeSets;
    private HashSet<GlobalKey> activeTransactions;
    private Map<Integer, Integer> contributionTally;

    enum ProcessorState {
        FREE, WAIT_ACCEPT, WAIT_COMMIT
    }

    private ProcessorState nodeState;

    private Node owner;

    private Printer printer;

    NodeTransactionProcessor(Node owner) {
        this.owner = owner;
        myShardId = owner.getShardId();
        nodeState = ProcessorState.FREE;

        retainerList = new ArrayList<NodeTransactionMessage>();
        transactionLog = new LinkedList<LocalKey>();
        transactionLogValues = new HashMap<LocalKey, Transaction>();
        committedLog = new HashMap<Integer, HashSet<LocalKey>>();
        nextTransaction = null;

        rewardedLog = new HashMap<Integer, HashSet<Integer>>();
        completedSets = new HashMap<Integer, Map<TransactionType, MinimalSet>>();
        activeSets = new HashMap<Integer, Map<TransactionType, HashSet<Integer>>>();
        activeTransactions = new HashSet<GlobalKey>();
        contributionTally = new HashMap<Integer, Integer>();
        requests = new HashMap<LocalKey, NodeTransactionMessage>();

        acceptTally = new NodeTally(owner);
        commitTally = new NodeTally(owner);
        timekeeper = new TimeKeeper();
        printer = owner.getPrinter();
    }

    void init() {
        var queueSize = ConfigData.getInt("platform.queueSize");
        inputQueue = new ManagedQueue(new PriorityBlockingQueue<NodeTransactionMessage>(), "Node input");
        outputQueue = new ManagedQueue(new ArrayBlockingQueue<NodeTransactionMessage>(queueSize), "Node output");
        requestQueue = new ManagedQueue(new PriorityBlockingQueue<NodeTransactionMessage>(queueSize), "Request input");

        var platforms = owner.entities.get(EntityType.PLATFORM);
        for (var platform : platforms.keySet()) {
            committedLog.put(platform, new HashSet<LocalKey>());
            rewardedLog.put(platform, new HashSet<Integer>());
            completedSets.put(platform, new HashMap<TransactionType, MinimalSet>());
            activeSets.put(platform, new HashMap<TransactionType, HashSet<Integer>>());
            for (var ttype : TransactionType.values()) {
                completedSets.get(platform).put(ttype, new MinimalSet());
                activeSets.get(platform).put(ttype, new HashSet<Integer>());
            }
        }
    }

    @Override
    public void run() {
        while (owner.isRunning()) {
            if (shouldWait()) {
                if (nodeState == ProcessorState.WAIT_ACCEPT || nodeState == ProcessorState.WAIT_COMMIT) {
                    doWait(timekeeper.remainingTime());
                } else {
                    doWait(null);
                }
            }

            if (!owner.isRunning()) {
                break;
            }

            if (owner.isByzantine()) {
                doSingleByzantine();
            } else if (owner.isPrimary()) {
                doSinglePrimary();
            } else {
                doSingleNonPrimary();
            }

            if (timekeeper.isTimedOut()) {
                if (nodeState == ProcessorState.WAIT_ACCEPT) {
                    doAcceptTimedOut();
                } else if (nodeState == ProcessorState.WAIT_COMMIT) {
                    doCommitTimedOut();
                }
            }
        }
    }

    boolean shouldWait() {
        if (!inputQueue.isEmpty()) {
            return false;
        }

        if (nodeState == ProcessorState.FREE && !requestQueue.isEmpty()) {
            return false;
        }

        return true;
    }

    void doCommitTimedOut() {
        printer.error("commit timed out " + nextTransaction.getGlobalKey());

        changeProcessorState(ProcessorState.FREE);
        clearNext();
        timekeeper.clear();
    }

    void doAcceptTimedOut() {
        if (nextTransaction != null && (owner.isCrashOnly() || isSuperPrimary(nextTransaction))) {
            var maxDelay = ConfigData.getInt("platform.maxReproposeDelayMs");
            int reproposeDelay = (int) (Math.random() * maxDelay / 2) + maxDelay / 2;
            doSleep(reproposeDelay);
            printer.info("Adding timed-out request back to queue!");
            var request = recreateRequest(nextTransaction);
            if (request != null) {
                inputQueue.offer(request);
            }
        } else {
            printer.error("accept timed out " + nextTransaction.getGlobalKey());
        }

        changeProcessorState(ProcessorState.FREE);
        clearNext();
        timekeeper.clear();
    }

    private void doSingleNonPrimary() {
        NodeTransactionMessage message = null;

        synchronized (inputQueue) {
            if (inputQueue.isEmpty()) {
                return;
            }

            message = inputQueue.poll();
        }

        var transaction = message.transaction;

        if (!isValid(message)) {
            return;
        }

        if (!isNext(message)) {
            retainerList.add(message);
            return;
        }

        if (message.messageType == NodeMessageType.COMMIT) {
            doCommit(message.transaction);
            changeProcessorState(ProcessorState.FREE);
            clearNext();
            timekeeper.clear();
        } else if (nodeState == ProcessorState.FREE) {
            if (message.messageType == NodeMessageType.PROPOSE) {
                var acceptMsg = createFrom(message, NodeMessageType.ACCEPT);
                outputQueue.offer(acceptMsg);
                nextTransaction = transaction;
                changeProcessorState(ProcessorState.WAIT_COMMIT);
                timekeeper.start("commit");
            }
        } else if (nodeState == ProcessorState.WAIT_COMMIT) {
            // Resend Reply
            if (message.messageType == NodeMessageType.PROPOSE
                    && transaction.getGlobalKey().equals(nextTransaction.getGlobalKey())) {
                var acceptMsg = createFrom(message, NodeMessageType.ACCEPT);
                outputQueue.offer(acceptMsg);
                timekeeper.start("commit");
            }
        }
    }

    private void doSingleByzantine() {
        NodeTransactionMessage message = null;

        synchronized (inputQueue) {
            if (!inputQueue.isEmpty()) {
                message = inputQueue.poll();
            } else if (nodeState == ProcessorState.FREE && !requestQueue.isEmpty()) {
                message = requestQueue.poll();
            } else {
                return;
            }
        }

        if (!isValid(message)) {
            printer.info("msg not valid " + message.transaction.getGlobalKey() + " from " + message.senderId);
            return;
        }

        if (!isNext(message)) {
            printer.info("msg not next " + message.transaction.getGlobalKey() + " from " + message.senderId);
            retainerList.add(message);
            return;
        }

        var messageType = message.messageType;
        var transaction = message.transaction;

        if (messageType == NodeMessageType.COMMIT) {
            addCommit(message);
        } else if (nodeState == ProcessorState.FREE) {
            if (messageType == NodeMessageType.REQUEST) {
                requests.put(transaction.getLocalKey(myShardId), message);
                var proposeMessage = createFrom(message, NodeMessageType.PROPOSE);
                acceptTally.add(createFrom(proposeMessage, NodeMessageType.ACCEPT));
                outputQueue.offer(proposeMessage);

                nextTransaction = transaction;
                changeProcessorState(ProcessorState.WAIT_ACCEPT);
                timekeeper.start("accept");

            } else if (messageType == NodeMessageType.PROPOSE) {
                requests.put(transaction.getLocalKey(myShardId), message);
                var acceptMsgProposer = new NodeTransactionMessage(message);
                acceptMsgProposer.messageType = NodeMessageType.ACCEPT;
                var acceptMsgNode = createFrom(message, NodeMessageType.ACCEPT);
                acceptTally.add(acceptMsgProposer);
                acceptTally.add(acceptMsgNode);
                outputQueue.offer(acceptMsgNode);

                nextTransaction = transaction;
                changeProcessorState(ProcessorState.WAIT_ACCEPT);
                timekeeper.start("accept");
            }
        } else if (nodeState == ProcessorState.WAIT_ACCEPT) {
            if (messageType == NodeMessageType.ACCEPT) {
                acceptTally.add(message);
                if (acceptTally.check(message)) {
                    var nextLocalKey = transaction.getLocalKey(myShardId);
                    var commitTransaction = transaction.copy();
                    var commitMsg = createFrom(message, NodeMessageType.COMMIT);
                    commitTransaction.setGlobalKey(acceptTally.getGlobalKey(nextLocalKey));
                    commitMsg.transaction = commitTransaction;
                    commitMsg.setHashes(acceptTally.getCompiledHashes(nextLocalKey));
                    outputQueue.offer(commitMsg);
                    changeProcessorState(ProcessorState.WAIT_COMMIT);
                    addCommit(commitMsg);
                } else {
                    timekeeper.start("accept");
                }
            }
        } else if (nodeState == ProcessorState.WAIT_COMMIT) {
            if (message.messageType == NodeMessageType.PROPOSE
                    && transaction.getGlobalKey().equals(nextTransaction.getGlobalKey())) {
                var acceptMsg = createFrom(message, NodeMessageType.ACCEPT);
                outputQueue.offer(acceptMsg);
            }

            timekeeper.start("commit");
        }
    }

    public void addCommit(NodeTransactionMessage message) {
        var transaction = message.transaction;

        commitTally.add(message);
        if (commitTally.check(message)) {

            if (nextTransaction != null && owner.getSuperPrimary(nextTransaction) == getId()) {
                if (!transaction.getGlobalKey().matches(nextTransaction.getGlobalKey())
                        && (nodeState == ProcessorState.WAIT_ACCEPT || nodeState == ProcessorState.WAIT_COMMIT)) {
                    inputQueue.offer(recreateRequest(nextTransaction));
                }
            }

            var nextLocalKey = transaction.getLocalKey(myShardId);
            var commitTransaction = transaction.copy();
            commitTransaction.setGlobalKey(commitTally.getGlobalKey(nextLocalKey));
            ////////////////////////////////////////////
            if (nextTransaction != null) {
                if (transaction.getGlobalKey().matches(nextTransaction.getGlobalKey())
                        && nodeState == ProcessorState.WAIT_ACCEPT) {

                    var commitMsg = createFrom(message, NodeMessageType.COMMIT);
                    commitMsg.transaction = commitTransaction;
                    commitMsg.setHashes(commitTally.getCompiledHashes(nextLocalKey));
                    outputQueue.offer(commitMsg);
                }
            }

            if (isSuperPrimary(message.transaction)) {
                var commitMsg = createFrom(message, NodeMessageType.COMMIT);
                commitMsg.transaction = commitTransaction;
                commitMsg.setHashes(commitTally.getCompiledHashes(nextLocalKey));
                commitMsg.sendToPlatform = true;
                outputQueue.offer(commitMsg);
            }
            //////////////////////////////////////////////
            doCommit(commitTransaction);
            clearNext();
            changeProcessorState(ProcessorState.FREE);
            timekeeper.clear();
        } else if (nodeState == ProcessorState.WAIT_COMMIT) {
            nextTransaction = transaction;
            timekeeper.start("commit");
        }
    }

    private void doSinglePrimary() {
        NodeTransactionMessage message = null;

        synchronized (inputQueue) {
            if (!inputQueue.isEmpty()) {
                message = inputQueue.poll();
            } else if (nodeState == ProcessorState.FREE && !requestQueue.isEmpty()) {
                message = requestQueue.poll();
            } else {
                return;
            }
        }

        if (!isValid(message)) {
//            printer.info("msg not valid " + message.transaction.getGlobalKey() + " from " + message.senderId);
            return;
        }

        if (!isNext(message)) {
//            printer.info("msg not next " + message.transaction.getGlobalKey() + " from " + message.senderId);
            retainerList.add(message);
            return;
        }

        var messageType = message.messageType;
        var transaction = message.transaction;

        if (messageType == NodeMessageType.COMMIT) {
            if (nextTransaction != null && owner.getSuperPrimary(nextTransaction) == getId()) {
                if (!transaction.getGlobalKey().matches(nextTransaction.getGlobalKey())
                        && (nodeState == ProcessorState.WAIT_ACCEPT || nodeState == ProcessorState.WAIT_COMMIT)) {
                    requestQueue.offer(recreateRequest(nextTransaction));
                }
            }

            doCommit(transaction);
            changeProcessorState(ProcessorState.FREE);
            clearNext();
            timekeeper.clear();
        } else if (nodeState == ProcessorState.FREE) {
            if (messageType == NodeMessageType.REQUEST) {
                requests.put(transaction.getLocalKey(myShardId), message);
                var proposeMessage = createFrom(message, NodeMessageType.PROPOSE);
                acceptTally.add(createFrom(proposeMessage, NodeMessageType.ACCEPT));
                outputQueue.offer(proposeMessage);

                nextTransaction = transaction;
                changeProcessorState(ProcessorState.WAIT_ACCEPT);
                timekeeper.start("accept");

            } else if (messageType == NodeMessageType.PROPOSE) {
                var shardProposeMsg = createFrom(message, NodeMessageType.PROPOSE);
                outputQueue.offer(shardProposeMsg);

                var acceptMsg = createFrom(message, NodeMessageType.ACCEPT);
                outputQueue.offer(acceptMsg);

                nextTransaction = transaction;
                changeProcessorState(ProcessorState.WAIT_COMMIT);
                timekeeper.start("commit");
            }
        } else if (nodeState == ProcessorState.WAIT_ACCEPT) {
            if (messageType == NodeMessageType.ACCEPT) {
                acceptTally.add(message);
                if (acceptTally.check(message)) {
                    var nextLocalKey = transaction.getLocalKey(myShardId);
                    var commitTransaction = transaction.copy();
                    var commitMsg = createFrom(message, NodeMessageType.COMMIT);
                    commitTransaction.setGlobalKey(acceptTally.getGlobalKey(nextLocalKey));
                    commitMsg.transaction = commitTransaction;
                    commitMsg.setHashes(acceptTally.getCompiledHashes(nextLocalKey));
                    outputQueue.offer(commitMsg);
                    doCommit(commitTransaction);
                    changeProcessorState(ProcessorState.FREE);
                    clearNext();
                    timekeeper.clear();
                } else {
                    timekeeper.start("accept");
                }
            }
        } else if (nodeState == ProcessorState.WAIT_COMMIT) {
            if (message.messageType == NodeMessageType.PROPOSE
                    && transaction.getGlobalKey().equals(nextTransaction.getGlobalKey())) {
                var shardProposeMsg = createFrom(message, NodeMessageType.PROPOSE);
                outputQueue.offer(shardProposeMsg);

                var acceptMsg = createFrom(message, NodeMessageType.ACCEPT);
                outputQueue.offer(acceptMsg);
                timekeeper.start("commit");
            }
        }
    }

    private void doCommit(Transaction transaction) {
//        printer.info("Committing " + transaction.getGlobalKey());
        var platforms = transaction.getPlatforms();
        var globalKey = transaction.getGlobalKey();
        var localKey = globalKey.getLocalKey(myShardId);
        var type = localKey.getType();

        transactionLog.add(localKey);
        transactionLogValues.put(localKey, transaction);
        for (var platform : platforms) {
            committedLog.get(platform).add(transaction.getLocalKey(platform));
        }

        var isComplete = false;
        TransactionType newType = null;
        var newNums = 0;

        int requiredContribution = ConfigData.getInt("platform.requiredContribution");

        if (type == TransactionType.REWARD) {
            for (var platform : platforms) {
                var pid = transaction.getLocalKey(platform).getId();
                rewardedLog.get(platform).add(pid);
            }

            contributionTally.remove(localKey.getId());
            isComplete = true;
        } else if (type == TransactionType.CONTRIBUTION) {
            var count = contributionTally.get(localKey.getId());
            count = count == null ? 1 : count + 1;
            if (count < requiredContribution) {
                contributionTally.put(localKey.getId(), count);
            } else {
                isComplete = true;
                newType = TransactionType.REWARD;
                newNums = 1;
            }
        } else {
            isComplete = true;
            newType = TransactionType.CONTRIBUTION;
            newNums = requiredContribution;
        }

        if (newType != null) {
            for (var i = 0; i < newNums; i++) {
                var newGlobal = globalKey.copy();
                newGlobal.setType(newType);
                newGlobal.setNum(i);
                activeTransactions.add(newGlobal);
            }

            for (var platform : platforms) {
                var pid = transaction.getLocalKey(platform).getId();
                activeSets.get(platform).get(newType).add(pid);
            }
        }

        if (isComplete) {
            activeTransactions.remove(globalKey);

            for (var platform : platforms) {
                var pid = transaction.getLocalKey(platform).getId();
                var cset = completedSets.get(platform).get(type);
                cset.add(pid);
                activeSets.get(platform).get(type).remove(pid);
            }
        }

        requests.remove(transaction.getLocalKey(myShardId));

        requestQueue.addAll(retainerList);
        retainerList.clear();

//        System.out.println("TransactionLog");
//        for (var item : transactionLog) {
//            System.out.println(item + " : " + transactionLogValues.get(item).getGlobalKey() + " : "
//                    + Printer.toString(transactionLogValues.get(item).getDigest(true)));
//        }
//        System.out.println("................");
    }

    void changeProcessorState(ProcessorState newState) {
        nodeState = newState;
        if (owner.isByzantine()) {
            inputQueue.addAll(retainerList);
            retainerList.clear();
        }
    }

    void clearNext() {
        if (nextTransaction != null) {
            acceptTally.remove(nextTransaction.getLocalKey(myShardId));
            commitTally.remove(nextTransaction.getLocalKey(myShardId));
        }

        if (owner.isCrashOnly()) {
            inputQueue.addAll(retainerList);
            retainerList.clear();
        }

        nextTransaction = null;
    }

    private NodeTransactionMessage createFrom(NodeTransactionMessage original, NodeMessageType newType) {
        var message = new NodeTransactionMessage(original);
        message.senderId = getId();
        message.timestamp = System.currentTimeMillis();
        message.messageType = newType;

        if (owner.isPrimary() || owner.isByzantine()) {
            if (!transactionLog.isEmpty()) {
                var lastKey = transactionLog.getLast();
                var lastTransaction = transactionLogValues.get(lastKey);
                message.setHash(myShardId, lastTransaction.getDigest(true));
            }

            message.digest = message.transaction.getDigest(false);
        }

        return message;
    }

    NodeTransactionMessage recreateRequest(Transaction transaction) {
        var message = requests.get(transaction.getLocalKey(myShardId));
        if (message == null) {
            printer.error("Message not found. " + transaction.getGlobalKey());
        }

//        if (committedLog.get(myShardId).contains(transaction.getLocalKey(myShardId))) {
//            var issuedTransactionIds = owner.getIssuedTransactionIds();
//            int nextId;
//            do {
//                nextId = issuedTransactionIds.next();
//                transaction.setLocalId(myShardId, nextId);
//            } while (committedLog.get(myShardId).contains(transaction.getLocalKey(myShardId)));
//
//            var key = message.getGlobalKey();
//            owner.getAssignedKeys().put(key, nextId);
//            issuedTransactionIds.add(nextId);
//        }

        message.timestamp = System.currentTimeMillis();
        message.senderId = getId();
        message.messageType = NodeMessageType.REQUEST;

        message.transaction = transaction.copy();
        message.digest = message.transaction.getDigest(false);
        return message;
    }

    boolean isAlreadyCommitted(Transaction transaction) {
        for (var platform : transaction.getPlatforms()) {
            var localKey = transaction.getLocalKey(platform);
            if (committedLog.get(platform).contains(localKey)) {
                return true;
            }
        }

        return false;
    }

    private boolean isValid(NodeTransactionMessage message) {
        var platforms = message.transaction.getPlatforms();

        if (!platforms.contains(myShardId)) {
            printer.error("Request sent to non-involved shard!");
            return false;
        }

        if (message.messageType == NodeMessageType.REQUEST) {
            if (!owner.isPrimary()) {
                printer.error("Request sent to non-primary node.");
                return false;
            }

            return true;
        }

        var transaction = message.transaction;
        var msgShardId = owner.getShardId(message.senderId);
        var msgPrimary = owner.getPrimaryNodeId(msgShardId);
        var msgLocalKey = transaction.getLocalKey(myShardId);

        if (message.messageType == NodeMessageType.PROPOSE) {
            if (msgPrimary != message.senderId) {
                printer.error("Propose received from non-primary node.");
                return false;
            }
        } else if (message.messageType == NodeMessageType.ACCEPT) {
            if (owner.isCrashOnly() && !owner.isPrimary()) {
                printer.error("(Crash Only) Accept sent to non-primary node.");
                return false;
            }
        } else if (message.messageType == NodeMessageType.COMMIT) {
            if (owner.isCrashOnly() && msgPrimary != message.senderId) {
                printer.error("(Crash Only) Commit received from non-primary node.");
                return false;
            }
        }

        if (getId() == message.senderId) {
            printer.error("Message sent from node to itself");
            return false;
        }

//        int expectedHashCount = 0;
//        int expectedIdCount = 0;
//
//        if (platforms.size() == 1) {
//            expectedHashCount = 1;
//            expectedIdCount = 1;
//        } else {
//            if (message.messageType == NodeMessageType.PROPOSE) {
//                if (myShardId == msgSuperShard) {
//                    expectedHashCount = 1;
//                } else {
//                    expectedHashCount = 2;
//                }
//
//                if (myShardId == msgSuperShard) {
//                    expectedIdCount = 1;
//                } else {
//                    expectedIdCount = 2;
//                }
//            } else if (message.messageType == NodeMessageType.ACCEPT) {
//                if (myShardId == msgShardId) {
//                    expectedHashCount = 1;
//                } else {
//                    expectedHashCount = 2;
//                }
//
//                if (myShardId == msgSuperShard && myShardId == msgShardId) {
//                    expectedIdCount = 1;
//                } else {
//                    expectedIdCount = 2;
//                }
//            } else if (message.messageType == NodeMessageType.COMMIT) {
//                expectedHashCount = platforms.size();
//                expectedIdCount = expectedHashCount;
//            }
//
//            if (transaction.getType() != TransactionType.SUBMISSION) {
//                expectedIdCount = platforms.size();
//            }
//        }
//
//        if (transaction.getType() == TransactionType.SUBMISSION) {
//            if (msgLocalKey == null || msgLocalKey.getId() == 0) {
//                expectedHashCount -= 1;
//            }
//
//            if (msgSuperShard != myShardId && (transaction.getLocalKey(msgSuperShard) == null
//                    || transaction.getLocalKey(msgSuperShard).getId() == 0)) {
//                expectedHashCount -= 1;
//            }
//
//            if (expectedHashCount < 0) {
//                expectedHashCount = 0;
//            }
//        }
//
//        if (message.getHashes().size() != expectedHashCount) {
//            printer.error(
//                    "Incorrect hash count. expected: " + expectedHashCount + ", found: " + message.getHashes().size());
//            return false;
//        }
//
//        if (transaction.getType() != TransactionType.REWARD && transaction.getKeySize() != expectedIdCount) {
//            printer.error("Incorrect id count. expected: " + expectedIdCount + ", found: " + transaction.getKeySize()
//                    + ", key: " + transaction.getGlobalKey());
//            return false;
//        }

        if (transaction.getType() != TransactionType.SUBMISSION || message.messageType != NodeMessageType.PROPOSE) {
            if (msgLocalKey == null) {
                printer.error("Local Id Not Found!");
                return false;
            }
        }

        if (isAlreadyCommitted(message.transaction)) {
//            printer.error("already committed " + message.transaction.getGlobalKey());
            return false;
        }

        if (!isValidState(message.transaction)) {
//            printer.error("not valid state " + message.transaction.getGlobalKey());
            return false;
        }

        if (message.messageType == NodeMessageType.PROPOSE && owner.isPrimary()) {
            //
        } else if (owner.isByzantine()) {
            //
        } else {
            if (!validateHash(message)) {
                printer.error("Invalid Hash!");
                return false;
            }
        }

        if (owner.isCrashOnly() && message.messageType == NodeMessageType.ACCEPT) {
            if (nextTransaction == null) {
                printer.error("Transacton Processed!");
                return false;
            }

            if (!nextTransaction.getGlobalKey().matches(message.transaction.getGlobalKey())) {
                printer.error("Transaction not matching!");
                return false;
            }
        }

        return true;
    }

    boolean isValidState(Transaction transaction) {
        var ttype = transaction.getType();

        for (var platform : transaction.getPlatforms()) {
            if (transaction.getLocalKey(platform) == null) {
                continue;
            }

            if (rewardedLog.get(platform).contains(transaction.getLocalKey(platform).getId())) {
                printer.error("Old message! " + transaction + " , " + rewardedLog);
                return false;
            }

            var cset = completedSets.get(platform).get(ttype);
            if (cset.isEmpty()) {
                continue;
            }

            var ptid = transaction.getLocalKey(platform).getId();
            if (cset.contains(ptid)) {
                printer.error("Old message ID! " + transaction + " , " + cset);
                return false;
            }

            var aset = activeSets.get(platform).get(ttype);
            if (aset.contains(ptid)) {
                for (var at : activeTransactions) {
                    if (at.matches(transaction.getGlobalKey())) {
                        return true;
                    }
                }

                printer.error("Invalid State!");
                return false;
            }
        }

        return true;
    }

    boolean validateHash(NodeTransactionMessage message) {
        var msgHash = message.getHash(myShardId);

        if (transactionLog.isEmpty()) {
            if (msgHash != null) {
                System.out.println("MSG HASH " + Printer.bytesToString(msgHash));
            }
            return msgHash == null;
        }

        var lastTransaction = transactionLogValues.get(transactionLog.getLast());

        var lastHash = lastTransaction.getDigest(true);
        var match = Arrays.equals(msgHash, lastHash);
        if (!match) {
            printer.error("MSG HASH " + Printer.bytesToString(msgHash) + " , " + "LAST HASH "
                    + Printer.bytesToString(lastHash));
        }

        return match;
    }

    boolean retainerCheck = false;

    private boolean isNext(NodeTransactionMessage message) {
        var transaction = message.transaction;
        var messageType = message.messageType;
        var key = transaction.getGlobalKey();
        if (messageType == NodeMessageType.REQUEST) {
            //////////////////////////////////////////////////////////////////
            if (retainerCheck == true) {
                retainerCheck = false;
            } else {
                if (!retainerList.isEmpty()) {
                    if (retainerCheck == false) {
                        retainerCheck = true;
                        printer.info(".");
                        inputQueue.addAll(retainerList);
                        printer.info(".");
                        retainerList.clear();
                        return false;

                    }
                }
            }

            /////////////////////////////////////////////////////////////////
            if (transaction.getType() == TransactionType.SUBMISSION) {
                var result = nodeState == ProcessorState.FREE;
                return result;
            }

            Boolean result2 = null;
            for (var at : activeTransactions) {
                if (at.matches(key)) {
                    result2 = nodeState == ProcessorState.FREE;
                    break;
                }
            }
            if (result2 != null) {
                return result2;
            }
        }

        if (messageType == NodeMessageType.PROPOSE) {
            var result = nextTransaction == null && nodeState == ProcessorState.FREE;
            return result;
        } else if (messageType == NodeMessageType.ACCEPT) {
            var result = nodeState == ProcessorState.WAIT_ACCEPT && key.matches(nextTransaction.getGlobalKey());
            return result;
        } else if (messageType == NodeMessageType.COMMIT) {

            if (!validateHash(message)) {
                return false;
            }

            var result = transaction.getType() == TransactionType.SUBMISSION
                    || transaction.getType() == TransactionType.REWARD || activeTransactions.contains(key);
            return result;
        }

        return true;
    }

    private int getId() {
        return owner.getId();
    }

    public ProcessorState getNodeState() {
        return nodeState;
    }

    public Node getOwner() {
        return owner;
    }

    public boolean isSuperPrimary(Transaction transaction) {
        return owner.getSuperPrimary(transaction) == getId();
    }

    private void doWait(Long duration) {
        synchronized (this) {
            try {
                if (duration == null) {
                    this.wait();
                } else {
                    this.wait(duration);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void doNotify() {
        synchronized (this) {
            this.notifyAll();
        }
    }

    private static void doSleep(long interval) {
        try {
            Thread.sleep(interval);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class TimeKeeper {
        private long dueTime;

        TimeKeeper() {
            dueTime = Long.MAX_VALUE;
        }

        private void start(String key) {
            var currentTime = System.currentTimeMillis();
            var acceptTimeoutMs = ConfigData.getInt("platform.acceptTimeoutMs");
            var commitTimeoutMs = ConfigData.getInt("platform.commitTimeoutMs");
            var timeoutMs = key.equals("accept") ? acceptTimeoutMs : commitTimeoutMs;
            dueTime = currentTime + timeoutMs;
        }

        private boolean isTimedOut() {
            return System.currentTimeMillis() >= dueTime;
        }

        private long remainingTime() {
            return dueTime - System.currentTimeMillis();
        }

        private void clear() {
            dueTime = Long.MAX_VALUE;
        }
    }

    void start() {
        Thread thread = new Thread(this);
        thread.start();
    }
}
