package com.separ.data.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.separ.utils.Printer;

public class NodeTransactionMessage extends EntityMessageBase implements Comparable<NodeTransactionMessage> {

    public enum NodeMessageType {
        COMMIT, ACCEPT, PROPOSE, REQUEST
    };

    public NodeMessageType messageType;

    // Shard ID -> Hash
    private Map<Integer, byte[]> hashes;

    public byte[] digest;

    // Used in byzantine mode
    public boolean sendToPlatform = false;

    private int localOrder;
    public int senderId;

    public NodeTransactionMessage() {
        hashes = new HashMap<Integer, byte[]>();
    }

    public NodeTransactionMessage(NodeTransactionMessage other) {
        super(other);

        messageType = other.messageType;
        hashes = new HashMap<Integer, byte[]>();
        hashes.putAll(other.hashes);
        digest = Arrays.copyOf(other.digest, other.digest.length);
        senderId = other.senderId;
        localOrder = other.localOrder;
    }

    public Map<Integer, byte[]> getHashes() {
        return hashes;
    }

    public void setHashes(Map<Integer, byte[]> hashes) {
        this.hashes = hashes;
    }

    public byte[] getHash(int shardId) {
        return hashes.get(shardId);
    }

    public void setHash(int shardId, byte[] hash) {
        hashes.put(shardId, hash);
    }

    public int getLocalOrder() {
        return localOrder;
    }

    public void setLocalOrder(int localOrder) {
        this.localOrder = localOrder;
    }

    public static NodeTransactionMessage fromEntityMessage(EntityTransactionMessage entityMessage) {
        var nodeMessage = new NodeTransactionMessage();
        nodeMessage.timestamp = System.currentTimeMillis();
        nodeMessage.messageType = NodeMessageType.REQUEST;
        nodeMessage.transaction = entityMessage.transaction.copy();
        nodeMessage.digest = entityMessage.transaction.getDigest(false);

        var entityKey = entityMessage.getGlobalKey();
        if (entityKey != null) {
            nodeMessage.globalKey = entityMessage.getGlobalKey().copy();
        }

        return nodeMessage;
    }

    @Override
    String getMessageLabel() {
        return transaction.getType().label() + ':' + messageType;
    }

    @Override
    void toByteArray2(DataOutputStream outstream) throws IOException {
        outstream.writeInt(messageType.ordinal());
        outstream.writeInt(hashes.size());
        for (var entry : hashes.entrySet()) {
            outstream.writeInt(entry.getKey());
            byte[] hash = entry.getValue();
            outstream.writeInt(hash.length);
            outstream.write(hash);
        }

        if (digest == null) {
            outstream.writeInt(0);
        } else {
            outstream.writeInt(digest.length);
            outstream.write(digest);
        }

        outstream.writeInt(senderId);
    }

    @Override
    void fromByteArray2(DataInputStream instream) throws IOException {
        messageType = NodeMessageType.values()[instream.readInt()];

        var hashCount = instream.readInt();
        if (hashCount > 0) {
            hashes = new HashMap<Integer, byte[]>();
            for (var i = 0; i < hashCount; i++) {
                var hashId = instream.readInt();
                var hashSize = instream.readInt();

                var hash = new byte[hashSize];
                instream.read(hash);
                hashes.put(hashId, hash);
            }
        }

        var digestLen = instream.readInt();
        if (digestLen == 0) {
            digest = null;
        } else {
            digest = new byte[digestLen];
            instream.read(digest);
        }

        senderId = instream.readInt();
    }

    @Override
    public int compareTo(NodeTransactionMessage other) {
        if (messageType != other.messageType) {
            return messageType.compareTo(other.messageType);
        }

        var cmp = super.compareTo(other);
        if (cmp != 0) {
            return cmp;
        }

        return Integer.compare(localOrder, other.localOrder);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(digest);
        result = prime * result + Objects.hash(hashes, localOrder, messageType, senderId);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        NodeTransactionMessage other = (NodeTransactionMessage) obj;
        return Arrays.equals(digest, other.digest) && Objects.equals(hashes, other.hashes)
                && localOrder == other.localOrder && messageType == other.messageType && senderId == other.senderId;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(super.toString());

        sb.append(", sender=");
        sb.append(senderId);
        sb.append(", participants=");
        sb.append(transaction.getParticipants().toString().replaceAll(" ", ""));
        if (!hashes.isEmpty()) {
            sb.append(", hashes=");
            sb.append(Printer.hashesToString(hashes));
        }
        sb.append(")");
        return sb.toString();
    }

}
