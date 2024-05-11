package com.separ.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EntityKey implements Comparable<EntityKey> {

    private int entityId;
    private EntityType entityType;

    public EntityKey() {

    }

    public EntityKey(int entityId, EntityType entityType) {
        this.entityId = entityId;
        this.setEntityType(entityType);
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
        EntityKey other = (EntityKey) obj;
        return entityId == other.entityId && entityType == other.entityType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + entityId;
        result = prime * result + entityType.ordinal();

        return result;
    }

    @Override
    public int compareTo(EntityKey other) {
        if (entityId != other.entityId) {
            return Integer.compare(entityId, other.entityId);
        }

        return Integer.compare(entityType.ordinal(), other.entityType.ordinal());
    }

    @Override
    public String toString() {
        return entityType.symbol() + entityId;
    }

}
