package com.separ.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityManager {

    private static Map<EntityType, Map<Integer, EntityInfo>> entities;
    private static Map<EntityType, Map<Integer, String>> states;

    static {
        entities = new HashMap<EntityType, Map<Integer, EntityInfo>>();
        states = new HashMap<EntityType, Map<Integer, String>>();

        for (var entityType : EntityType.values()) {
            entities.put(entityType, new HashMap<Integer, EntityInfo>());
            states.put(entityType, new HashMap<Integer, String>());
        }
    }

    public static void put(EntityInfo entity, String state) {
        entities.get(entity.type()).put(entity.getId(), entity);
        states.get(entity.type()).put(entity.getId(), state);
    }

    public static EntityInfo get(EntityType entityType, int entityId) {
        return entities.get(entityType).get(entityId);
    }

    public static List<EntityInfo> getAll() {
        return entities.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toList());
    }

    public static Map<EntityType, Map<Integer, EntityInfo>> getEntityMap() {
        return entities;
    }

    public static String getState(EntityType entityType, int entityId) {
        return states.get(entityType).get(entityId);
    }

    public static int countState(EntityType entityType, String state) {
        if (states.containsKey(entityType)) {
            return (int) states.get(entityType).values().stream().filter(x -> x.equals(state)).count();
        }

        return 0;
    }
}
