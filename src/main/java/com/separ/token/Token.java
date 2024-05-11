package com.separ.token;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.separ.entity.EntityInfo;
import com.separ.entity.EntityType;
import com.separ.transaction.Transaction;
import com.separ.utils.CryptoUtils;
import com.separ.utils.Printer;
import com.separ.utils.RACryptoUtils;

public class Token {

    private int id;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    private static Random random = new Random(); // Used in RA

    public Token() {

    }

    public Token(Token other) {
        this.id = other.id;
        this.privateKey = other.privateKey;
        this.publicKey = other.publicKey;
    }

    // Used in RA
    static Token generate(Map<EntityType, Integer> participants) {
        var token = new Token();

        var timestamp = System.currentTimeMillis();

        var nonce = new byte[16];
        random.nextBytes(nonce);

        token.publicKey = new PublicKey(nonce, timestamp);
        token.privateKey = new PrivateKey(nonce, timestamp, participants);

        return token;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    boolean matches(Transaction transaction) {
        if (privateKey == null) {
            return false;
        }

        var participants = transaction.getParticipants();
        var tokenParticipants = privateKey.getParticipants();

        var workerFound = false;

        for (var participant : participants) {
            var entityType = participant.getEntityType();
            var entityId = participant.getEntityId();

            if (tokenParticipants.containsKey(entityType)) {
                if (tokenParticipants.get(entityType) == entityId) {
                    if (entityType == EntityType.WORKER) {
                        workerFound = true;
                    }
                } else {
                    if (entityType == EntityType.REQUESTER || entityType == EntityType.PLATFORM) {
                        return false;
                    }
                }
            }
        }

        if (tokenParticipants.containsKey(EntityType.WORKER)) {
            return workerFound;
        }

        return true;
    }

    boolean isParticipant(EntityInfo entity) {
        var participants = privateKey.getParticipants();
        return participants.containsKey(entity.type()) && participants.get(entity.type()) == entity.getId();
    }

    boolean isInitiator(EntityInfo entity) {
        if (!isParticipant(entity)) {
            return false;
        }

        var targets = privateKey.getParticipants().keySet();
        if (targets.contains(EntityType.WORKER)) {
            return entity.type() == EntityType.WORKER;
        }

        if (targets.contains(EntityType.REQUESTER)) {
            return entity.type() == EntityType.REQUESTER;
        }

        return entity.type() == EntityType.PLATFORM;
    }

    public byte[] getBytes(boolean includePrivateKey) {
        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeInt(id);

            var publicBytes = publicKey.getBytes(true);
            outstream.writeInt(publicBytes.length);
            outstream.write(publicBytes);

            if (includePrivateKey) {
                if (privateKey == null) {
                    outstream.writeInt(0);
                } else {
                    var privateBytes = privateKey.getBytes(true);
                    outstream.writeInt(privateBytes.length);
                    outstream.write(privateBytes);
                }
            }

            return bytestream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void fromBytes(byte[] data, boolean includePrivateKey) {

        var bytestream = new ByteArrayInputStream(data);
        var instream = new DataInputStream(bytestream);
        try {
            id = instream.readInt();

            var publicLen = instream.readInt();
            var publicBytes = new byte[publicLen];
            instream.read(publicBytes);
            publicKey = new PublicKey();
            publicKey.fromBytes(publicBytes, true);

            if (includePrivateKey) {
                var privateLen = instream.readInt();
                if (privateLen > 0) {
                    var privateBytes = new byte[privateLen];
                    instream.read(privateBytes);
                    privateKey = new PrivateKey();
                    privateKey.fromBytes(privateBytes, true);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        if (privateKey == null) {
            return "Token #" + id + " (PublicKey=" + publicKey + ")";

        } else {
            return "Token #" + id + " (PrivateKey=" + privateKey + ", PublicKey=" + publicKey + ")";
        }
    }

    public String toString(boolean summarized) {
        if (summarized) {
            var str = "Token #" + id + " (nonce=" + Printer.bytesToString(publicKey.nonce) + ", t="
                    + publicKey.timestamp;
            if (privateKey != null) {
                str += ", participants=" + Printer.entitiesToString(privateKey.getParticipants());
            }

            str += ")";
            return str;
        } else {
            return toString();
        }
    }

    private static class PublicKey {
        private byte[] nonce;
        private long timestamp;
        private byte[] signature;

        public PublicKey() {

        }

        public PublicKey(byte[] nonce, long timestamp) {
            this.nonce = nonce;
            this.timestamp = timestamp;

            var bytes = getBytes(false);
            signature = CryptoUtils.sign(bytes, RACryptoUtils.getSignaturePrivateKey());
        }

        public byte[] getNonce() {
            return nonce;
        }

        public void setNonce(byte[] nonce) {
            this.nonce = nonce;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public byte[] getSignature() {
            return signature;
        }

        public void setSignature(byte[] signature) {
            this.signature = signature;
        }

        public byte[] getBytes(boolean includeSignature) {

            var bytestream = new ByteArrayOutputStream();
            var outstream = new DataOutputStream(bytestream);
            try {
                outstream.writeInt(nonce.length);
                outstream.write(nonce);
                outstream.writeLong(timestamp);
                if (includeSignature) {
                    outstream.writeInt(signature.length);
                    outstream.write(signature);
                }
                return bytestream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        public void fromBytes(byte[] data, boolean includeSignature) {

            var bytestream = new ByteArrayInputStream(data);
            var instream = new DataInputStream(bytestream);
            try {
                var nonceLen = instream.readInt();
                nonce = new byte[nonceLen];
                instream.read(nonce);
                timestamp = instream.readLong();
                if (includeSignature) {
                    var signatureLen = instream.readInt();
                    signature = new byte[signatureLen];
                    instream.read(signature);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "[nonce=" + Printer.bytesToString(nonce) + ", t=" + timestamp + ", signature="
                    + Printer.bytesToString(signature) + "]";
        }
    }

    public static class PrivateKey {
        private byte[] nonce;
        private long timestamp;
        private Map<EntityType, Integer> participants;
        private byte[] signature;

        public PrivateKey() {

        }

        public PrivateKey(byte[] nonce, long timestamp, Map<EntityType, Integer> participants) {
            this.nonce = nonce;
            this.timestamp = timestamp;
            this.participants = participants;

            var bytes = getBytes(false);
            signature = CryptoUtils.sign(bytes, RACryptoUtils.getSignaturePrivateKey());
        }

        public byte[] getNonce() {
            return nonce;
        }

        public void setNonce(byte[] nonce) {
            this.nonce = nonce;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public Map<EntityType, Integer> getParticipants() {
            return participants;
        }

        public void setParticipants(Map<EntityType, Integer> participants) {
            this.participants = participants;
        }

        public byte[] getSignature() {
            return signature;
        }

        public void setSignature(byte[] signature) {
            this.signature = signature;
        }

        public byte[] getBytes(boolean includeSignature) {

            var bytestream = new ByteArrayOutputStream();
            var outstream = new DataOutputStream(bytestream);
            try {
                outstream.writeInt(nonce.length);
                outstream.write(nonce);
                outstream.writeLong(timestamp);
                outstream.writeInt(participants.size());
                for (var participant : participants.entrySet()) {
                    outstream.writeInt(participant.getKey().ordinal());
                    outstream.writeInt(participant.getValue());
                }
                if (includeSignature) {
                    outstream.writeInt(signature.length);
                    outstream.write(signature);
                }
                return bytestream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        public void fromBytes(byte[] data, boolean includeSignature) {
            var bytestream = new ByteArrayInputStream(data);
            var instream = new DataInputStream(bytestream);
            try {
                var nonceLen = instream.readInt();
                nonce = new byte[nonceLen];
                instream.read(nonce);
                timestamp = instream.readLong();
                var participantSize = instream.readInt();
                participants = new HashMap<EntityType, Integer>();
                for (var i = 0; i < participantSize; i++) {
                    var key = EntityType.values()[instream.readInt()];
                    var value = instream.readInt();
                    participants.put(key, value);
                }
                if (includeSignature) {
                    var signatureLen = instream.readInt();
                    signature = new byte[signatureLen];
                    instream.read(signature);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "[nonce=" + Printer.bytesToString(nonce) + ", t=" + timestamp + ", participants="
                    + Printer.entitiesToString(participants) + ", signature=" + Printer.bytesToString(signature) + "]";
        }
    }
}
