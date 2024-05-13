package com.separ.token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.separ.entity.EntityInfo;
import com.separ.entity.EntityType;
import com.separ.utils.Printer;

public class Constraint {
    private int id;
    private List<EntityType> targets;
    private int threshold;
    private static int nextId = 0;

    public Constraint(int id, List<EntityType> targets, int threshold) {
        this.id = id;
        this.targets = targets;
        this.threshold = threshold;
    }

    // Used in RA
    public static Constraint generate(List<EntityType> targets, int threshold) {
        var constraint = new Constraint(nextId, targets, threshold);
        nextId++;
        return constraint;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<EntityType> getTargets() {
        return targets;
    }

    public int getThreshold() {
        return threshold;
    }

    public List<Future<Token>> generateTokens(Map<EntityType, Map<Integer, EntityInfo>> entities,
            ExecutorService taskExecutor) {
        var futures = new ArrayList<Future<Token>>();

        var participantsList = new ArrayList<Map<EntityType, Integer>>();
        var runner = new ArrayList<Map<EntityType, Integer>>();

        for (var target : targets) {
            var entityIds = entities.get(target).keySet();
            if (participantsList.isEmpty()) {
                for (var entityId : entityIds) {
                    var participants = new HashMap<EntityType, Integer>();
                    participants.put(target, entityId);
                    runner.add(participants);
                }
            } else {
                for (var participants : participantsList) {
                    for (var entityId : entityIds) {
                        var ps2 = new HashMap<EntityType, Integer>(participants);
                        ps2.put(target, entityId);
                        runner.add(ps2);
                    }
                }
            }

            participantsList = runner;
            runner = new ArrayList<Map<EntityType, Integer>>();
        }

        for (var participants : participantsList) {
            for (var i = 0; i < threshold; i++) {
                futures.add(taskExecutor.submit(() -> Token.generate(participants)));
            }
        }

        return futures;
    }

    public List<Token> getTokens(List<Token> tokens, EntityInfo entity) {
        var entityTokens = new ArrayList<Token>();
        for (var token : tokens) {
            if (token.isParticipant(entity)) {
                var entityToken = new Token(token);
                if (!token.isInitiator(entity)) {
                    //entityToken.setPrivateKey(null);
                }
                entityTokens.add(entityToken);
            }
        }

        return entityTokens;
    }

    // TODO: Enables adding / removing entities dynamically.
    public boolean getTokens(EntityInfo oldEntity, EntityInfo newEntity) {
        return false;
    }

    public boolean applies(EntityInfo entity) {
        return targets.contains(entity.type());
    }

    public boolean applies(EntityType type) {
        return targets.contains(type);
    }

    public boolean isInitiator(EntityInfo entity) {
        if (!applies(entity)) {
            return false;
        }

        if (targets.contains(EntityType.WORKER)) {
            return entity.type() == EntityType.WORKER;
        }

        if (targets.contains(EntityType.REQUESTER)) {
            return entity.type() == EntityType.REQUESTER;
        }

        return entity.type() == EntityType.PLATFORM;
    }

    @Override
    public String toString() {
        var typesStr = Printer.entityTypesToString(targets);
        return "Constraint #" + id + " (targets=" + typesStr + ", threshold=" + threshold + ")";
    }

}
