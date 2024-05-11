package com.separ.transaction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.separ.utils.CodecInterface;

public class LocalKey implements CodecInterface, Comparable<LocalKey> {

    private int id;
    private TransactionType type;
    private int num;

    public LocalKey() {
    }

    LocalKey(int id, TransactionType type, int num) {
        this.id = id;
        this.type = type;
        this.setNum(num);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    @Override
    public byte[] toByteArray() {

        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeInt(type.ordinal());
            outstream.writeInt(id);
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
            id = instream.readInt();
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
        LocalKey other = (LocalKey) obj;
        return id == other.id && type == other.type && num == other.num;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + type.ordinal();
        result = prime * result + id;
        result = prime * result + num;

        return result;
    }

    @Override
    public int compareTo(LocalKey other) {
        if (type != other.type) {
            return type.compareTo(other.type);
        }

        if (num != other.num) {
            return Integer.compare(num, other.num);
        }

        return Integer.compare(id, other.id);
    }

    @Override
    public String toString() {
        return "LocalKey [" + type + ", " + id + ", " + num + "]";
    }

}
