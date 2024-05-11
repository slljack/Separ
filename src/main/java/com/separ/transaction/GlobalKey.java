package com.separ.transaction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.TreeMap;

import com.separ.utils.CodecInterface;

public class GlobalKey implements CodecInterface, Comparable<GlobalKey> {

    private TreeMap<Integer, Integer> idMap;
    private TransactionType type;
    private int num;

    public GlobalKey() {
        idMap = new TreeMap<Integer, Integer>();
    }

    int getLocalId(int platformId) {
        return idMap.get(platformId);
    }

    void setLocalId(int platformId, int id) {
        idMap.put(platformId, id);
    }

    public TreeMap<Integer, Integer> getIdMap() {
        return idMap;
    }

    public LocalKey getLocalKey(int platformId) {
        if (idMap.containsKey(platformId)) {
            return new LocalKey(getLocalId(platformId), type, num);
        }

        return null;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public void merge(GlobalKey other) {
        if (type == null) {
            type = other.getType();
        }

        if (idMap == null) {
            idMap = new TreeMap<Integer, Integer>();
        }

        idMap.putAll(other.getIdMap());
    }

    @Override
    public byte[] toByteArray() {

        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeInt(type.ordinal());
            outstream.writeInt(idMap.size());
            for (var key : idMap.keySet()) {
                outstream.writeInt(key);
                outstream.writeInt(idMap.get(key));
            }
            outstream.writeInt(num);
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
            type = TransactionType.values()[instream.readInt()];
            var mapSize = instream.readInt();
            for (var i = 0; i < mapSize; i++) {
                idMap.put(instream.readInt(), instream.readInt());
            }
            num = instream.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        GlobalKey other = (GlobalKey) obj;
        if (type != other.type) {
            return false;
        }

        var otherMap = other.idMap;
        if (idMap.size() != otherMap.size()) {
            return false;
        }

        for (var entry : idMap.entrySet()) {
            if (!otherMap.containsKey(entry.getKey()) || otherMap.get(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }

        if (num != other.num) {
            return false;
        }

        return true;
    }

    public boolean matches(GlobalKey other) {
        var matching = new HashSet<>(idMap.keySet());
        matching.retainAll(other.getIdMap().keySet());

        for (var platform : matching) {
            if (other.getLocalId(platform) != getLocalId(platform)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + type.ordinal();
        for (var entry : idMap.entrySet()) {
            result = prime * result + entry.getKey();
            result = prime * result + entry.getValue();
        }

        result = prime * result + num;

        return result;
    }

    @Override
    public int compareTo(GlobalKey other) {
        if (type != other.type) {
            return type.compareTo(other.type);
        }

        if (num != other.num) {
            return Integer.compare(num, other.num);
        }

        for (var entry : this.idMap.entrySet()) {
            if (other.idMap.containsKey(entry.getKey())) {
                var int1 = entry.getValue();
                var int2 = other.idMap.get(entry.getKey());
                if (int1 != int2) {
                    return Integer.compare(int1, int2);
                }
            }
        }

        return 0;
    }

    public GlobalKey copy() {
        var cp = new GlobalKey();

        cp.idMap = new TreeMap<Integer, Integer>(idMap);
        cp.type = type;
        cp.num = num;

        return cp;
    }

    @Override
    public String toString() {
        if (idMap.isEmpty() && type == TransactionType.SUBMISSION) {
            return "request";
        }

        var sb = new StringBuilder();
        sb.append('t');
        var first = true;
        for (var entry : idMap.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(entry.getKey());
            sb.append('.');
            sb.append(entry.getValue());
        }

        if (type != TransactionType.SUBMISSION) {
            sb.append(type.symbol());
        }

        if (type == TransactionType.CONTRIBUTION) {
            sb.append(num);
        }

        return sb.toString();
    }
}
