package com.separ.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MessageKey implements Comparable<MessageKey> {

    private int messageId;
    private int entityId;
    private EntityType entityType;

    public MessageKey() {
        entityType = EntityType.PLATFORM;
    }

    MessageKey(int messageId, int entityId, EntityType entityType) {
        this.messageId = messageId;
        this.entityId = entityId;
        this.setEntityType(entityType);
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int id) {
        this.messageId = id;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public byte[] toByteArray() {

        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeInt(messageId);
            outstream.writeInt(entityId);
            outstream.writeInt(entityType.ordinal());
            return bytestream.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void fromByteArray(byte[] data) {

        var bytestream = new ByteArrayInputStream(data);
        var instream = new DataInputStream(bytestream);
        try {
            messageId = instream.readInt();
            entityId = instream.readInt();
            entityType = EntityType.values()[instream.readInt()];
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
        MessageKey other = (MessageKey) obj;
        return messageId == other.messageId && entityId == other.entityId && entityType == other.entityType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + messageId;
        result = prime * result + entityId;
        result = prime * result + entityType.ordinal();

        return result;
    }

    @Override
    public int compareTo(MessageKey other) {

        if (messageId != other.messageId) {
            return Integer.compare(messageId, other.messageId);
        }

        if (entityId != other.entityId) {
            return Integer.compare(entityId, other.entityId);
        }

        return Integer.compare(entityType.ordinal(), other.entityType.ordinal());
    }

    public MessageKey copy() {
        return new MessageKey(messageId, entityId, entityType);
    }

    @Override
    public String toString() {
        return entityType.symbol() + entityId + '#' + messageId;
    }

}
