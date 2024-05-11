package com.separ.data.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.separ.transaction.Transaction;

public class EntityTransactionMessage extends EntityMessageBase implements Comparable<EntityTransactionMessage> {

    public EntityTransactionMessage() {

    }

    public EntityTransactionMessage(EntityTransactionMessage other) {
        super(other);
    }

    public static EntityTransactionMessage fromNodeMessage(NodeTransactionMessage nodeMessage) {
        var entityMessage = new EntityTransactionMessage();
        entityMessage.timestamp = System.currentTimeMillis();
        entityMessage.transaction = nodeMessage.transaction.copy();

        if (nodeMessage.getGlobalKey() != null) {
            entityMessage.globalKey = nodeMessage.getGlobalKey().copy();
        }

        return entityMessage;
    }

    public static EntityTransactionMessage fromEntityMessage(EntityTransactionMessage oldMessage,
            Transaction transaction) {
        var newMessage = new EntityTransactionMessage(oldMessage);
        newMessage.timestamp = System.currentTimeMillis();
        newMessage.transaction = transaction;
        return newMessage;
    }

    @Override
    public int compareTo(EntityTransactionMessage other) {
        return super.compareTo(other);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(super.toString());

        sb.append(", participants=");
        sb.append(transaction.getParticipants().toString().replaceAll(" ", ""));
        sb.append(")");
        return sb.toString();
    }

    @Override
    void toByteArray2(DataOutputStream outstream) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void fromByteArray2(DataInputStream instream) throws IOException {
        // TODO Auto-generated method stub

    }
}
