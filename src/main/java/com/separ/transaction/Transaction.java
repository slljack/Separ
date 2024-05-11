package com.separ.transaction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.separ.entity.EntityKey;
import com.separ.entity.EntityType;
import com.separ.utils.CommonUtils;

public class Transaction implements Comparable<Transaction> {

    GlobalKey key;

    List<EntityKey> participants;

    public long timestamp;

    public List<Integer> platforms;

    public GlobalKey getGlobalKey() {
        return key;
    }

    public void setGlobalKey(GlobalKey key) {
        this.key = key;
    }

    public LocalKey getLocalKey(int platformId) {
        return key.getLocalKey(platformId);
    }

    public void setLocalId(int platformId, int localId) {
        if (key == null) {
            key = new GlobalKey();
        }

        key.setLocalId(platformId, localId);
    }

    public int getKeySize() {
        return key.getIdMap().size();
    }

    public void setType(TransactionType type) {
        key.setType(type);
    }

    public TransactionType getType() {
        return key.getType();
    }

    public List<EntityKey> getParticipants() {
        return participants;
    }

    public Integer getParticipantId(EntityType type) {
        for (var participant : participants) {
            if (participant.getEntityType() == type) {
                return participant.getEntityId();
            }
        }

        return null;
    }

    public EntityKey getParticipant(EntityType type) {
        return new EntityKey(getParticipantId(type), type);
    }

    public void setParticipants(List<EntityKey> participants) {
        this.participants = participants;
    }

    public void addParticipant(EntityType type, int entityId) {
        participants.add(new EntityKey(entityId, type));
    }

    public List<Integer> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<Integer> platforms) {
        this.platforms = platforms;
    }

    public byte[] toByteArray() {
        return toByteArray(true);
    }

    public byte[] toByteArray(boolean includeId) {
        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            if (includeId) {
                CommonUtils.write(outstream, key);
            }

            if (participants == null) {
                outstream.writeInt(0);
            } else {
                outstream.writeInt(participants.size());
                for (var participant : participants) {
                    var bytes = participant.toByteArray();
                    outstream.writeInt(bytes.length);
                    outstream.write(bytes);
                }
            }

            outstream.writeLong(timestamp);
            CommonUtils.write(outstream, platforms);

            toByteArray2(outstream);

            return bytestream.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void toByteArray2(DataOutputStream outstream) throws IOException {

    }

    public void fromByteArray(byte[] data) {

        var bytestream = new ByteArrayInputStream(data);
        var instream = new DataInputStream(bytestream);
        try {
            key = CommonUtils.readCodec(instream, new GlobalKey());

            var participantCount = instream.readInt();
            if (participantCount > 0) {
                participants = new ArrayList<EntityKey>();
                for (var i = 0; i < participantCount; i++) {
                    var participant = new EntityKey();
                    var itemlen = instream.readInt();
                    var bytes = new byte[itemlen];
                    instream.read(bytes);
                    participant.fromByteArray(bytes);
                    participants.add(participant);
                }
            }

            timestamp = instream.readLong();
            platforms = CommonUtils.readList(instream);

            fromByteArray2(instream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fromByteArray2(DataInputStream instream) throws IOException {

    }

    public byte[] getDigest(boolean includeId) {

        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(this.toByteArray(includeId));
            return digest;
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public Transaction copy() {
        var cp = new Transaction();
        cp.key = key == null ? null : key.copy();
        cp.participants = new ArrayList<EntityKey>(participants);
        cp.timestamp = timestamp;
        cp.platforms = List.copyOf(platforms);

        return cp;
    }

    @Override
    public int compareTo(Transaction other) {
        var cmp = key.compareTo(other.key);
        if (cmp == 0) {
            cmp = Long.compare(timestamp, other.timestamp);
        }

        return cmp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, participants, platforms, timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Transaction other = (Transaction) obj;
        return Objects.equals(key, other.key) && Objects.equals(participants, other.participants)
                && Objects.equals(platforms, other.platforms) && timestamp == other.timestamp;
    }

    public String toString(boolean summarized) {
        if (summarized) {
            return key.toString();
        } else {
            return toString();
        }
    }

    @Override
    public String toString() {
        return "Transaction [key=" + key + ", participants=" + participants + ", timestamp=" + timestamp
                + ", platforms=" + platforms + "]";
    }

}
