package com.separ.flow;

import java.util.Map;

import com.separ.flow.StateTracker.StateEvent;

public abstract class StateMachineRunner implements Runnable {
    private String state;
    protected boolean waiting;

    public StateMachineRunner() {
        state = "init";
        waiting = false;
    }

    public void setState(String state) {
        var oldState = this.state;
        if (!state.equals(oldState)) {
            this.state = state;
            StateTracker.fireStateChanged(new StateEvent(null, oldState, state));

            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    public String getState() {
        return state;
    }

    protected abstract Map<String, Runnable> getStateMachine();

    @Override
    public void run() {
        var stateMachine = getStateMachine();

        while (true) {
            // Wait for a signal
            while (waiting) {
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            var method = stateMachine.get(state);
            if (method != null) {
                method.run();
            }

            if (state.equals("stop")) {
                break;
            }
        }
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }
}
