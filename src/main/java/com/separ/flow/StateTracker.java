package com.separ.flow;

import java.util.HashSet;
import java.util.Set;

import com.separ.entity.EntityInfo;

public class StateTracker {
    private static Set<StateListener> listeners = new HashSet<>();

    public static void addStateListener(StateListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public static void removeStateListener(StateListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    static void fireStateChanged(StateEvent stateEvent) {
        Set<StateListener> snapshot;
        synchronized (listeners) {
            snapshot = new HashSet<StateListener>(listeners);
        }

        for (StateListener listener : snapshot) {
            listener.stateChanged(stateEvent);
        }
    }

    public static class StateEvent {

        public final EntityInfo target;
        public final String oldState;
        public final String newState;

        StateEvent(EntityInfo target, String oldState, String newState) {
            this.target = target;
            this.oldState = oldState;
            this.newState = newState;
        }
    }

    public static interface StateListener {
        void stateChanged(StateEvent event);
    }
}
