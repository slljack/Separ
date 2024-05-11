package com.separ.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.separ.token.Token;

public class CommonUtils {

    public static <T extends CodecInterface> void write(DataOutputStream outstream, T item) throws IOException {
        if (item == null) {
            outstream.writeInt(0);
        } else {
            var bytes = item.toByteArray();
            outstream.writeInt(bytes.length);
            if (bytes.length > 0) {
                outstream.write(bytes);
            }
        }
    }

    public static void write(DataOutputStream outstream, List<Integer> list) throws IOException {
        outstream.writeInt(list.size());
        for (var item : list) {
            outstream.writeInt(item);
        }
    }

    public static <T extends CodecInterface> T readCodec(DataInputStream instream, T item) throws IOException {
        var len = instream.readInt();
        if (len == 0) {
            return null;
        }

        var bytes = new byte[len];
        instream.read(bytes);
        item.fromByteArray(bytes);
        return item;
    }

    public static List<Integer> readList(DataInputStream instream) throws IOException {

        var count = instream.readInt();
        if (count == 0) {
            return null;
        }

        var list = new ArrayList<Integer>();
        for (var i = 0; i < count; i++) {
            list.add(instream.readInt());
        }

        return list;
    }

    public static byte[] joinBytes(byte[] first, byte[] other) {

        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeInt(first.length);
            outstream.write(first);
            outstream.writeInt(other.length);
            outstream.write(other);

            return bytestream.toByteArray();
        } catch (IOException e) {
        }

        return null;
    }

    public static HashMap<Integer, Token> toMap(List<Token> tokens) {
        var map = new HashMap<Integer, Token>();
        for (var token : tokens) {
            map.put(token.getId(), token);
        }

        return map;
    }
}
