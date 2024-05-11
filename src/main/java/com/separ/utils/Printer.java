package com.separ.utils;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.separ.entity.EntityInfo;
import com.separ.entity.EntityType;
import com.separ.token.Token;

public class Printer {

    private String prefix;

    private boolean enabled = true;
    private static Encoder encoder = Base64.getEncoder();

    public Printer(String id) {
        if (id == null) {
            prefix = "";
        } else {
            prefix = "[" + id + "] ";
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void info(Object obj) {
        if (enabled) {
            System.out.println(prefix + obj.toString());
        }
    }

    public void error(Object obj) {
        if (enabled) {
            System.err.println(prefix + "ERROR: " + obj.toString());
        }
    }

    public void out(Object obj) {
        if (enabled) {
            System.out.println(prefix + ">> " + obj.toString());
        }
    }

    public void in(Object obj) {
        if (enabled) {
            System.out.println(prefix + "<< " + obj.toString());
        }
    }

    public void title(String label) {
        if (enabled) {
            info(titleString(label, 20));
        }
    }

    public static String titleString(String label, int len) {
        StringBuilder sb = new StringBuilder();
        var sidePad = (len - label.length()) / 2.0 - 1;
        for (int i = 0; i < Math.floor(sidePad); i++) {
            sb.append('=');
        }
        sb.append(' ');
        sb.append(label);
        sb.append(' ');
        for (int i = 0; i < Math.ceil(sidePad); i++) {
            sb.append('=');
        }

        return sb.toString();
    }

    public static String entityTypesToString(List<EntityType> list) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        var first = true;
        for (var entityType : list) {
            if (first) {
                first = false;
            } else {
                builder.append(',');
            }
            builder.append(entityType.symbol());
        }
        builder.append(']');
        return builder.toString();
    }

    public static String entitiesToString(Map<EntityType, Integer> map) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        var first = true;
        for (var entry : map.entrySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(',');
            }
            builder.append(entry.getKey().symbol() + entry.getValue());
        }
        builder.append(']');
        return builder.toString();
    }

    public static String entityMapToString(Map<EntityType, Map<Integer, EntityInfo>> map) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        var first = true;
        for (var key : map.keySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(',');
            }
            builder.append(key.id() + ":" + map.get(key).size());
        }
        builder.append(']');
        return builder.toString();
    }

    public static String tokensToString(Map<Integer, List<Token>> tokens) {
        StringBuilder builder = new StringBuilder();
        for (var key : tokens.keySet()) {
            builder.append("Constraint #");
            builder.append(key);

            var count = tokens.get(key).size();
            builder.append(" (" + count + " Tokens)");
            builder.append(":\n");

            for (var token : tokens.get(key).subList(0, 5)) {
                builder.append(token.toString(true));
                builder.append('\n');
            }

            if (count > 5) {
                builder.append("...\n");
            }
        }
        return builder.toString();
    }

    public static String tokenIDs(HashMap<Integer, Token> map) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        var first = true;
        for (var key : map.keySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(',');
            }
            builder.append("#" + map.get(key).getId());
        }
        builder.append(']');
        return builder.toString();

    }

    public static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        var encoded = encoder.encodeToString(bytes);
        return encoded.length() > 12 ? encoded.substring(0, 9) + "..." : encoded;
    }

    public static String hashesToString(Map<Integer, byte[]> hashes) {
        if (hashes == null) {
            return "";
        }

        var hexMap = new HashMap<Integer, String>();

        for (var entry : hashes.entrySet()) {
            hexMap.put(entry.getKey(), bytesToString(entry.getValue()));
        }

        return hexMap.toString();
    }
}
