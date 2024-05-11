package com.separ.token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.separ.entity.MessageKey;
import com.separ.transaction.Transaction;

public class Budget {

    // Constraint ID -> List of Tokens
    private Map<Integer, List<Token>> availableTokens;

    // Message ID -> Constraint ID -> Token
    private Map<MessageKey, Map<Integer, Token>> reserved;

    public Budget() {
        availableTokens = new HashMap<Integer, List<Token>>();
        reserved = new HashMap<MessageKey, Map<Integer, Token>>();
    }

    public void addToken(int constraintId, Token token) {
        if (!availableTokens.containsKey(constraintId)) {
            availableTokens.put(constraintId, new ArrayList<Token>());
        }

        availableTokens.get(constraintId).add(token);

    }

    public void addTokens(Map<Integer, List<Token>> newTokens) {
        for (var key : newTokens.keySet()) {
            if (availableTokens.containsKey(key)) {
                availableTokens.get(key).addAll(newTokens.get(key));
            } else {
                availableTokens.put(key, newTokens.get(key));
            }
        }
    }

    public boolean checkTokens(int constraintId, Transaction transaction, MessageKey globalKey) {
        Token match = null;
        int index = -1;
        var cTokens = availableTokens.get(constraintId);
        for (var i = 0; i < cTokens.size(); i++) {
            var token = cTokens.get(i);
            if (token.matches(transaction)) {
                match = token;
                index = i;
                break;
            }
        }

        if (match == null) {
            return false;
        }

        if (!reserved.containsKey(globalKey)) {
            reserved.put(globalKey, new HashMap<Integer, Token>());
        }

        reserved.get(globalKey).put(constraintId, match);
        cTokens.remove(index);
        return true;
    }

    public void releaseTokens(MessageKey globalKey) {
        for (var entry : reserved.get(globalKey).entrySet()) {
            var cid = entry.getKey();
            availableTokens.get(cid).add(entry.getValue());
        }
    }

    public Map<Integer, Token> spendTokens(MessageKey globalKey) {
        var tokens = reserved.get(globalKey);
        reserved.remove(globalKey);
        return tokens;
    }
}
