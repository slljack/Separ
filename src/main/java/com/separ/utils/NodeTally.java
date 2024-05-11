package com.separ.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.separ.config.ConfigData;
import com.separ.data.entity.NodeTransactionMessage;
import com.separ.entity.Node;
import com.separ.transaction.GlobalKey;
import com.separ.transaction.LocalKey;
import com.separ.transaction.TransactionType;

public class NodeTally {
    private Node owner;

    // Local Transaction Key -> Platform ID -> Global Transaction Key -> Hashes -> Count
    private HashMap<LocalKey, HashMap<Integer, HashMap<GlobalKey, HashMap<HashKey, Integer>>>> counter;

    private HashMap<LocalKey, GlobalKey> keyMap;

    private HashMap<LocalKey, Map<Integer, byte[]>> compiledHashes;

    private HashSet<LocalKey> accepted;

    public NodeTally(Node owner) {
        this.owner = owner;
        counter = new HashMap<>();
        keyMap = new HashMap<>();
        compiledHashes = new HashMap<>();
        accepted = new HashSet<>();
    }

    public void add(NodeTransactionMessage msg) {
        var transaction = msg.transaction;
        var localkey = transaction.getLocalKey(owner.getShardId());
        var senderShard = owner.getShardId(msg.senderId);
        var globalKey = transaction.getGlobalKey();

        if (!counter.containsKey(localkey)) {
            counter.put(localkey, new HashMap<Integer, HashMap<GlobalKey, HashMap<HashKey, Integer>>>());
        }

        var localMap = counter.get(localkey);
        if (!localMap.containsKey(senderShard)) {
            localMap.put(senderShard, new HashMap<GlobalKey, HashMap<HashKey, Integer>>());
        }

        var senderMap = localMap.get(senderShard);
        if (!senderMap.containsKey(globalKey)) {
            senderMap.put(globalKey, new HashMap<NodeTally.HashKey, Integer>());
        }

        var hashMap = senderMap.get(globalKey);
        var hashKey = new HashKey(msg.getHashes());
        var count = 0;

        if (hashMap.containsKey(hashKey)) {
            count = hashMap.get(hashKey);
        }

        hashMap.put(hashKey, count + 1);
//        owner.getPrinter().info("tally addition: " + localkey + " -> " + senderShard + " -> " + globalKey + " "
//                + msg.messageType + " - " + (count + 1));
    }

    public boolean check(NodeTransactionMessage msg) {
        var transaction = msg.transaction;
        var localKey = transaction.getLocalKey(owner.getShardId());
        var isReward = msg.transaction.getType() == TransactionType.REWARD;

        if (accepted.contains(localKey)) {
            // owner.getPrinter().info("tally yes " + transaction.getGlobalKey() + " " + msg.messageType);
            return true;
        }

        if (!counter.containsKey(localKey)) {
            return false;
        }

        var localMap = counter.get(localKey);

        var f = ConfigData.getInt("experiment.faultyCount");
        var requiredShardCount = isReward ? 1 : transaction.getPlatforms().size();
        var requiredPerShard = owner.isCrashOnly() ? (f + 1) : (2 * f + 1);

        if (localMap.size() < requiredShardCount) {
//            owner.getPrinter()
//                    .info("tally just " + transaction.getGlobalKey() + " " + msg.messageType + " / " + localMap.size());
            return false;
        }

        var globalKey = new GlobalKey();
        globalKey.setType(localKey.getType());
        globalKey.setNum(localKey.getNum());

        var acceptedCount = 0;
        var compiledHash = new HashMap<Integer, byte[]>();
        for (var shardId : localMap.keySet()) {
            var shardMap = localMap.get(shardId);
            var found = false;
            for (var shardKey : shardMap.keySet()) {
                var hashSet = shardMap.get(shardKey);
                for (var hashEntry : hashSet.entrySet()) {
                    var hashCount = hashEntry.getValue();
                    if (hashCount >= requiredPerShard) {
                        globalKey.merge(shardKey);
                        compiledHash.putAll(hashEntry.getKey().hashes);
                        acceptedCount += 1;
                        found = true;
                        break;
                    }
                }

                if (found) {
                    break;
                }
            }

        }

//        owner.getPrinter()
//                .info("tally count " + transaction.getGlobalKey() + " " + msg.messageType + " * " + acceptedCount);

        if (acceptedCount == requiredShardCount) {
            accepted.add(localKey);
            keyMap.put(localKey, globalKey);
            compiledHashes.put(localKey, compiledHash);
            counter.remove(localKey);
            return true;
        }

        return false;
    }

    public void remove(LocalKey localKey) {
        counter.remove(localKey);
    }

    public GlobalKey getGlobalKey(LocalKey localKey) {
        return keyMap.get(localKey).copy();
    }

    public Map<Integer, byte[]> getCompiledHashes(LocalKey localKey) {
        return compiledHashes.get(localKey);
    }

    private final class HashKey {
        private final Map<Integer, byte[]> hashes;

        private HashKey(Map<Integer, byte[]> hashes) {
            this.hashes = hashes;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;

            HashKey other = (HashKey) obj;
            return NodeUtils.equals(this.hashes, other.hashes);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            for (var entry : hashes.entrySet()) {
                result = prime * result + entry.getKey();
                result = prime * result + Arrays.hashCode(entry.getValue());
            }

            return result;
        }
    }
}
