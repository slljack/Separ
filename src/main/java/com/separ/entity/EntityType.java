package com.separ.entity;

import java.util.Map;

public enum EntityType {
    WORKER("worker"), REQUESTER("requester"), PLATFORM("platform"), NODE("node");

    private String id;

    EntityType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String symbol() {
        return name().substring(0, 1);
    }

    // Reverse Lookup

    private static final Map<String, EntityType> lookup = Map.of(REQUESTER.id(), REQUESTER, PLATFORM.id(), PLATFORM,
            WORKER.id(), WORKER, NODE.id(), NODE);

    public static EntityType get(String id) {
        return lookup.get(id);
    }
}
