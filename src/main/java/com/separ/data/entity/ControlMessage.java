package com.separ.data.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.separ.token.Token;
import com.separ.utils.Printer;

public class ControlMessage extends EntityMessageBase {

    public enum ControlMessageType {
        TOKENS_REQ, TOKENS_RES, SIGNATURE_REQ, SIGNATURE_RES
    }

    public ControlMessageType type;

    // Constraint ID -> Token
    public HashMap<Integer, Token> tokens;

    // Token ID -> Signature
    public Map<Integer, byte[]> tokenSignatures;
    public Map<Integer, byte[]> tokenTaskSignatures;

    public ControlMessage() {
        tokens = new HashMap<Integer, Token>();
        tokenSignatures = new HashMap<Integer, byte[]>();
        tokenTaskSignatures = new HashMap<Integer, byte[]>();
    }

    public ControlMessage(ControlMessage other) {
        super(other);
        type = other.type;
        tokens = new HashMap<Integer, Token>();
        tokens.putAll(other.tokens);
        tokenSignatures = new LinkedHashMap<Integer, byte[]>();
        tokenSignatures.putAll(other.tokenSignatures);
        tokenTaskSignatures = new LinkedHashMap<Integer, byte[]>();
        tokenTaskSignatures.putAll(other.tokenTaskSignatures);
    }

    @Override
    void toByteArray2(DataOutputStream outstream) throws IOException {
        outstream.writeInt(type.ordinal());

        if (tokens == null) {
            outstream.writeInt(0);
        } else {
            outstream.writeInt(tokens.size());
            for (var key : tokens.keySet()) {
                outstream.writeInt(key);
                var item = tokens.get(key);
                var bytes = item.getBytes(true);
                outstream.writeInt(bytes.length);
                outstream.write(bytes);
            }
        }

        if (tokenSignatures == null) {
            outstream.writeInt(0);
        } else {
            outstream.writeInt(tokenSignatures.size());
            for (var key : tokenSignatures.keySet()) {
                outstream.writeInt(key);
                var item = tokenSignatures.get(key);
                outstream.writeInt(item.length);
                outstream.write(item);
            }
        }

        if (tokenTaskSignatures == null) {
            outstream.writeInt(0);
        } else {
            outstream.writeInt(tokenTaskSignatures.size());
            for (var key : tokenTaskSignatures.keySet()) {
                outstream.writeInt(key);
                var item = tokenTaskSignatures.get(key);
                outstream.writeInt(item.length);
                outstream.write(item);
            }
        }
    }

    @Override
    void fromByteArray2(DataInputStream instream) throws IOException {
        type = ControlMessageType.values()[instream.readInt()];

        var keyCount = instream.readInt();
        if (keyCount > 0) {
            tokens = new HashMap<Integer, Token>();
            for (var i = 0; i < keyCount; i++) {
                var key = instream.readInt();
                var itemLen = instream.readInt();
                var itemBytes = new byte[itemLen];
                instream.read(itemBytes);
                var item = new Token();
                item.fromBytes(itemBytes, true);
                tokens.put(key, item);
            }
        }

        var signatureCount = instream.readInt();
        if (signatureCount > 0) {
            tokenSignatures = new HashMap<Integer, byte[]>();
            for (var i = 0; i < signatureCount; i++) {
                var key = instream.readInt();
                var itemLen = instream.readInt();
                var item = new byte[itemLen];
                instream.read(item);
                tokenSignatures.put(key, item);
            }
        }

        var signatureCount2 = instream.readInt();
        if (signatureCount2 > 0) {
            tokenTaskSignatures = new HashMap<Integer, byte[]>();
            for (var i = 0; i < signatureCount2; i++) {
                var key = instream.readInt();
                var itemLen = instream.readInt();
                var item = new byte[itemLen];
                instream.read(item);
                tokenTaskSignatures.put(key, item);
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(tokenSignatures, tokenTaskSignatures, tokens, type);
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
        ControlMessage other = (ControlMessage) obj;
        return Objects.equals(tokenSignatures, other.tokenSignatures)
                && Objects.equals(tokenTaskSignatures, other.tokenTaskSignatures)
                && Objects.equals(tokens, other.tokens) && type == other.type;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(super.toString());

        if (type != ControlMessageType.TOKENS_REQ) {
            sb.append(", tokens=");
            sb.append(Printer.tokenIDs(tokens));
        }

        if (type == ControlMessageType.SIGNATURE_RES) {
            sb.append(", tokenSig=");
            sb.append(Printer.hashesToString(tokenSignatures));
            sb.append(", tokenTaskSig=");
            sb.append(Printer.hashesToString(tokenTaskSignatures));
        }

        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getMessageLabel() {
        var typeLabelMap = Map.of(ControlMessageType.TOKENS_REQ, "Token Request", ControlMessageType.TOKENS_RES,
                "Token Response", ControlMessageType.SIGNATURE_REQ, "Signature Request",
                ControlMessageType.SIGNATURE_RES, "Signature Response");

        return typeLabelMap.get(type);
    }
}
