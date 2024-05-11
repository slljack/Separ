package com.separ.transaction;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.separ.entity.EntityKey;
import com.separ.entity.EntityType;
import com.separ.token.Token;

public class RewardTransaction extends Transaction {
    private List<Token> tokens;

    // Token ID -> Entity Type / Signature Group -> Signature
    private Map<Integer, Map<EntityType, byte[]>> tokenSignatures;

    // Token ID -> Entity Type / Signature Group -> Signature
    private Map<Integer, Map<EntityType, byte[]>> tokenTaskSignatures;

    public RewardTransaction() {
        tokenSignatures = new HashMap<Integer, Map<EntityType, byte[]>>();
        tokenTaskSignatures = new HashMap<Integer, Map<EntityType, byte[]>>();
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void addToken(Token token) {
        if (tokens == null) {
            tokens = new ArrayList<Token>();
        }

        tokens.add(token);
    }

    public Map<Integer, Map<EntityType, byte[]>> getTokenSignatures() {
        return tokenSignatures;
    }

    public void setTokenSignatures(Map<Integer, Map<EntityType, byte[]>> tokenSignatures) {
        this.tokenSignatures = tokenSignatures;
    }

    public Map<Integer, Map<EntityType, byte[]>> getTokenTaskSignatures() {
        return tokenTaskSignatures;
    }

    public void setTokenTaskSignatures(Map<Integer, Map<EntityType, byte[]>> tokenTaskSignatures) {
        this.tokenTaskSignatures = tokenTaskSignatures;
    }

    @Override
    public void toByteArray2(DataOutputStream outstream) throws IOException {

        if (tokens == null) {
            outstream.writeInt(0);
        } else {
            outstream.writeInt(tokens.size());
            for (var token : tokens) {
                var bytes = token.getBytes(false);
                outstream.writeInt(bytes.length);
                outstream.write(bytes);
            }
        }

        toByteArray3(outstream, tokenSignatures);
        toByteArray3(outstream, tokenTaskSignatures);
    }

    private void toByteArray3(DataOutputStream outstream, Map<Integer, Map<EntityType, byte[]>> signatureMap)
            throws IOException {

        outstream.writeInt(signatureMap.size());
        for (var tokenId : signatureMap.keySet()) {
            outstream.writeInt(tokenId);
            var tokenMap = signatureMap.get(tokenId);
            outstream.writeInt(tokenMap.size());
            for (var entityType : tokenMap.keySet()) {
                outstream.writeInt(entityType.ordinal());

                var signature = tokenMap.get(entityType);
                outstream.writeInt(signature.length);
                outstream.write(signature);
            }
        }
    }

    @Override
    public void fromByteArray2(DataInputStream instream) throws IOException {
        var tokenCount = instream.readInt();
        if (tokenCount > 0) {
            tokens = new ArrayList<Token>();
            for (var i = 0; i < tokenCount; i++) {
                var token = new Token();
                var tokenLen = instream.readInt();
                var bytes = new byte[tokenLen];
                instream.read(bytes);
                token.fromBytes(bytes, false);
                tokens.add(token);
            }
        }

        tokenSignatures = fromByteArray3(instream);

        tokenTaskSignatures = fromByteArray3(instream);
    }

    private Map<Integer, Map<EntityType, byte[]>> fromByteArray3(DataInputStream instream) throws IOException {
        var signatureMap = new HashMap<Integer, Map<EntityType, byte[]>>();
        var mapCount = instream.readInt();
        for (var i = 0; i < mapCount; i++) {
            var tokenId = instream.readInt();
            signatureMap.put(tokenId, new HashMap<EntityType, byte[]>());
            var mapSize = instream.readInt();
            for (var j = 0; j < mapSize; j++) {
                var entityType = EntityType.values()[instream.readInt()];
                var bytesLen = instream.readInt();
                var bytes = new byte[bytesLen];
                instream.read(bytes);
                signatureMap.get(tokenId).put(entityType, bytes);
            }
        }

        return signatureMap;
    }

    @Override
    public RewardTransaction copy() {
        var cp = new RewardTransaction();
        cp.key = key == null ? null : key.copy();
        cp.participants = new ArrayList<EntityKey>(participants);
        cp.timestamp = timestamp;
        cp.platforms = List.copyOf(platforms);

        cp.tokens = new ArrayList<Token>();
        cp.tokens.addAll(tokens);

        cp.tokenSignatures = new HashMap<Integer, Map<EntityType, byte[]>>();
        for (var tid : tokenSignatures.keySet()) {
            cp.tokenSignatures.put(tid, new HashMap<EntityType, byte[]>());
            cp.tokenSignatures.get(tid).putAll(tokenSignatures.get(tid));
        }

        cp.tokenTaskSignatures = new HashMap<Integer, Map<EntityType, byte[]>>();
        for (var tid : tokenTaskSignatures.keySet()) {
            cp.tokenTaskSignatures.put(tid, new HashMap<EntityType, byte[]>());
            cp.tokenTaskSignatures.get(tid).putAll(tokenTaskSignatures.get(tid));
        }

        return cp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(tokenSignatures, tokenTaskSignatures, tokens);
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
        RewardTransaction other = (RewardTransaction) obj;
        return Objects.equals(tokenSignatures, other.tokenSignatures)
                && Objects.equals(tokenTaskSignatures, other.tokenTaskSignatures)
                && Objects.equals(tokens, other.tokens);
    }

    @Override
    public String toString() {
        return "RewardTransaction [tokens=" + tokens + ", tokenSignatures=" + tokenSignatures + ", tokenTaskSignatures="
                + tokenTaskSignatures + ", key=" + key + ", participants=" + participants + ", timestamp=" + timestamp
                + ", platforms=" + platforms + "]";
    }
}
