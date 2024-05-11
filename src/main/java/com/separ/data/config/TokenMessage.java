package com.separ.data.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.separ.data.MessageInterface;
import com.separ.token.Token;

public class TokenMessage implements MessageInterface {

    private Map<Integer, List<Token>> tokens;

    public TokenMessage() {
        tokens = new HashMap<Integer, List<Token>>();
    }

    public TokenMessage(Map<Integer, List<Token>> tokens) {
        this.tokens = tokens;
    }

    public Map<Integer, List<Token>> getTokens() {
        return tokens;
    }

    @Override
    public byte[] toByteArray() {
        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeInt(tokens.size());
            for (var key : tokens.keySet()) {
                outstream.writeInt(key);
                var tokenList = tokens.get(key);
                outstream.writeInt(tokenList.size());
                for (var token : tokenList) {
                    var tbytes = token.getBytes(true);
                    outstream.writeInt(tbytes.length);
                    outstream.write(tbytes);
                }
            }

            return bytestream.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void fromByteArray(byte[] data) {

        var bytestream = new ByteArrayInputStream(data);
        var instream = new DataInputStream(bytestream);
        try {
            var entryCount = instream.readInt();
            for (var i = 0; i < entryCount; i++) {
                var cid = instream.readInt();
                var tokenCount = instream.readInt();
                var tokenList = new ArrayList<Token>();
                for (var j = 0; j < tokenCount; j++) {
                    var bytesLen = instream.readInt();
                    var bytes = new byte[bytesLen];
                    instream.read(bytes);
                    var token = new Token();
                    token.fromBytes(bytes, true);
                    tokenList.add(token);
                }

                tokens.put(cid, tokenList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
