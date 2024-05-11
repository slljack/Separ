package com.separ.transaction;

import java.util.Map;

public enum TransactionType {
    SUBMISSION("submission"), CONTRIBUTION("contribution"), REWARD("reward");

    private String id;

    TransactionType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String label() {
        return id.substring(0, 1).toUpperCase() + id.substring(1);
    }

    public String symbol() {
        return id.substring(0, 1);
    }

    // Reverse Lookup

    private static final Map<String, TransactionType> lookup = Map.of(REWARD.id(), REWARD, CONTRIBUTION.id(),
            CONTRIBUTION, SUBMISSION.id(), SUBMISSION);

    public static TransactionType get(String id) {
        return lookup.get(id);
    }
}
