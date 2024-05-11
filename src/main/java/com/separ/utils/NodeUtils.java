package com.separ.utils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.separ.config.ConfigData;
import com.separ.data.entity.NodeTransactionMessage;

public class NodeUtils {
    public static NodeTransactionMessage poll(BlockingQueue<NodeTransactionMessage> queue) {
        var queueTimeoutMs = ConfigData.getInt("platform.queueTimeoutMs");

        synchronized (queue) {
            try {
                var message = queue.poll(queueTimeoutMs, TimeUnit.MILLISECONDS);
                return message;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static void offer(BlockingQueue<NodeTransactionMessage> queue, NodeTransactionMessage item,
            String queueName) {
        var queueTimeoutMs = ConfigData.getInt("platform.queueTimeoutMs");

        synchronized (queue) {
            try {
                var result = queue.offer(item, queueTimeoutMs, TimeUnit.MILLISECONDS);
                if (!result) {
                    System.err.println(queueName + " queue is full. Exiting.");
                    System.exit(1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean equals(Map<Integer, byte[]> one, Map<Integer, byte[]> other) {
        if (one == other)
            return true;
        if (other == null)
            return false;
        if (one.size() != other.size())
            return false;

        for (var entry : one.entrySet()) {
            if (!other.containsKey(entry.getKey()) || !Arrays.equals(entry.getValue(), other.get(entry.getKey()))) {
                return false;
            }
        }

        return true;
    }
}
