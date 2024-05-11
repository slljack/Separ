package com.separ.utils;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.separ.data.entity.NodeTransactionMessage;

public class ManagedQueue {

    private BlockingQueue<NodeTransactionMessage> queue;
    private String name;

    public ManagedQueue(BlockingQueue<NodeTransactionMessage> queue, String name) {
        this.name = name;
        this.queue = queue;
    }

    public boolean isEmpty() {
        synchronized (queue) {
            return queue.isEmpty();
        }
    }

    public void addAll(List<NodeTransactionMessage> list) {
        synchronized (queue) {
            for (var item : list) {
                queue.offer(item);
            }
        }
    }

    public void offer(NodeTransactionMessage item) {
        synchronized (queue) {
            NodeUtils.offer(queue, item, name);
        }
    }

    public NodeTransactionMessage poll() {
        synchronized (queue) {
            return queue.poll();
        }
    }
}
