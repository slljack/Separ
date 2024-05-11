package com.separ.entity;

public class EntityInfo {
    private EntityType type;
    private int id;
    private String ip;

    public EntityInfo(EntityType type, int id, String ip) {
        this.type = type;
        this.id = id;
        this.ip = ip;
    }

    public EntityType type() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String toString() {
        return "EntityInfo [type=" + type + ", id=" + id + ", ip=" + ip + "]";
    }
}
