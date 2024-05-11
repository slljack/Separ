package com.separ.data.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

import com.separ.data.MessageInterface;
import com.separ.entity.MessageKey;
import com.separ.transaction.RewardTransaction;
import com.separ.transaction.Transaction;

public abstract class EntityMessageBase implements MessageInterface {
    MessageKey globalKey;
    MessageKey localKey;
    public Transaction transaction;
    public long timestamp;

    EntityMessageBase() {

    }

    EntityMessageBase(EntityMessageBase other) {
        timestamp = other.timestamp;
        globalKey = other.globalKey.copy();
        localKey = other.localKey.copy();
        transaction = other.transaction.copy();
    }

    public MessageKey getGlobalKey() {
        return globalKey;
    }

    public void setGlobalKey(MessageKey globalKey) {
        this.globalKey = globalKey;
    }

    public MessageKey getLocalKey() {
        return localKey;
    }

    public void setLocalKey(MessageKey localKey) {
        this.localKey = localKey;
    }

    String getMessageLabel() {
        return transaction.getType().label();
    }

    @Override
    public byte[] toByteArray() {
        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeLong(timestamp);

            if (globalKey == null) {
                outstream.writeInt(0);
            } else {
                var ebytes = globalKey.toByteArray();
                outstream.writeInt(ebytes.length);
                outstream.write(ebytes);
            }

            if (localKey == null) {
                outstream.writeInt(0);
            } else {
                var ebytes = localKey.toByteArray();
                outstream.writeInt(ebytes.length);
                outstream.write(ebytes);
            }

            if (transaction == null) {
                outstream.writeInt(0);
            } else {
                var tbytes = transaction.toByteArray();
                outstream.writeInt(tbytes.length);
                outstream.write(tbytes);

                if (transaction instanceof RewardTransaction) {
                    outstream.writeByte((byte) 'R');
                } else {
                    outstream.writeByte((byte) 'T');
                }
            }

            toByteArray2(outstream);

            return bytestream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    abstract void toByteArray2(DataOutputStream outstream) throws IOException;

    @Override
    public void fromByteArray(byte[] data) {
        var bytestream = new ByteArrayInputStream(data);
        var instream = new DataInputStream(bytestream);
        try {
            timestamp = instream.readLong();

            var psize = instream.readInt();
            if (psize == 0) {
                globalKey = null;
            } else {
                var pbytes = new byte[psize];
                instream.read(pbytes);
                globalKey = new MessageKey();
                globalKey.fromByteArray(pbytes);
            }

            var esize = instream.readInt();
            if (esize > 0) {
                var ebytes = new byte[esize];
                instream.read(ebytes);
                localKey = new MessageKey();
                localKey.fromByteArray(ebytes);
            }

            var tsize = instream.readInt();
            if (tsize == 0) {
                transaction = null;
            } else {
                var tbytes = new byte[tsize];
                instream.read(tbytes);

                char magicNumber = (char) instream.readUnsignedByte();
                if (magicNumber == 'R') {
                    transaction = new RewardTransaction();
                } else {
                    transaction = new Transaction();
                }

                transaction.fromByteArray(tbytes);
            }

            fromByteArray2(instream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract void fromByteArray2(DataInputStream instream) throws IOException;

    int compareTo(EntityMessageBase other) {
        var cmp = transaction.compareTo(other.transaction);
        if (cmp != 0) {
            return cmp;
        }

        return globalKey.compareTo(other.globalKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localKey, globalKey, timestamp, transaction);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EntityMessageBase other = (EntityMessageBase) obj;
        return Objects.equals(localKey, other.localKey) && Objects.equals(globalKey, other.globalKey)
                && timestamp == other.timestamp && Objects.equals(transaction, other.transaction);
    }

    @Override
    public String toString() {

        var sb = new StringBuilder();
        sb.append(getMessageLabel());
        sb.append(" (transaction=");
        sb.append(transaction.toString(true));
        sb.append(", platforms=");
        sb.append(transaction.getPlatforms());
        if (localKey != null) {
            sb.append(", localKey=");
            sb.append(localKey);
        }

        if (globalKey != null) {
            sb.append(", globalKey=");
            sb.append(globalKey);
        }

        return sb.toString();
    }
}
