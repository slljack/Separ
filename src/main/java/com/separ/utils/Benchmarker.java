package com.separ.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.separ.data.entity.EntityMessageBase;
import com.separ.entity.MessageKey;
import com.separ.transaction.GlobalKey;
import com.separ.transaction.Transaction;

public class Benchmarker {

    // Measurement Name -> Start Time
    private Map<String, Long> unitTimekeeper;

    // Measurement Name -> Duration
    private Map<String, Long> unitDurations;

    // Measurement Name -> Transaction ID -> Start Time
    private Map<String, Map<GlobalKey, Long>> transactionTimekeeper;

    // Measurement Name -> Transaction ID -> Duration
    private Map<String, Map<GlobalKey, Long>> transactionDurations;

    // Measurement Name -> Task ID -> Start Time
    private Map<String, Map<MessageKey, Long>> taskTimekeeper;

    // Measurement Name -> Task ID -> Duration
    private Map<String, Map<MessageKey, Long>> taskDurations;

    private int completedCount = 0;
    private int queueCount = 0;
    private Map<Long, Integer> queueRecord;

    boolean printed = false;

    private String label;

    public Benchmarker(String label, int run) {
        unitTimekeeper = new LinkedHashMap<String, Long>();
        unitDurations = new LinkedHashMap<String, Long>();

        transactionTimekeeper = new LinkedHashMap<String, Map<GlobalKey, Long>>();
        transactionDurations = new LinkedHashMap<String, Map<GlobalKey, Long>>();

        taskTimekeeper = new LinkedHashMap<String, Map<MessageKey, Long>>();
        taskDurations = new LinkedHashMap<String, Map<MessageKey, Long>>();

        queueRecord = new LinkedHashMap<Long, Integer>();

        this.label = label;
    }

//    public void init(String key, String itemId, Long startTime) {
//        _get(timekeeper, key).put(itemId, startTime);
//    }
    public void start(String measurement) {
        unitTimekeeper.put(measurement, System.currentTimeMillis());
    }

    public void end(String measurement) {
        if (!unitTimekeeper.containsKey(measurement)) {
            return;
        }

        var start = unitTimekeeper.get(measurement);
        unitDurations.put(measurement, System.currentTimeMillis() - start);
        unitTimekeeper.remove(measurement);
    }

    public void start(String measurement, Transaction t) {
        var key = t.getGlobalKey();
        if (!transactionTimekeeper.containsKey(measurement)) {
            transactionTimekeeper.put(measurement, new HashMap<GlobalKey, Long>());
            transactionDurations.put(measurement, new HashMap<GlobalKey, Long>());
        }

        transactionTimekeeper.get(measurement).put(key, System.currentTimeMillis());
    }

    public void end(String measurement, Transaction t) {
        var key = t.getGlobalKey();

        var start = transactionTimekeeper.get(measurement).get(key);
        transactionDurations.get(measurement).put(key, System.currentTimeMillis() - start);
        transactionTimekeeper.get(measurement).remove(key);
    }

    public void start(String measurement, EntityMessageBase msg) {
        var key = msg.getGlobalKey();
        if (!taskTimekeeper.containsKey(measurement)) {
            taskTimekeeper.put(measurement, new HashMap<MessageKey, Long>());
            taskDurations.put(measurement, new HashMap<MessageKey, Long>());
        }

        taskTimekeeper.get(measurement).put(key, System.currentTimeMillis());

        if (measurement.equals("process")) {
            if (queueRecord.isEmpty()) {
                queueRecord.put(System.currentTimeMillis(), 1);
            }

            queueCount += 1;
        }
    }

    public void end(String measurement, EntityMessageBase msg) {
        var key = msg.getGlobalKey();

        if (!taskTimekeeper.containsKey(measurement) || !taskTimekeeper.get(measurement).containsKey(key)) {
            return;
        }

        var start = taskTimekeeper.get(measurement).get(key);
        taskDurations.get(measurement).put(key, System.currentTimeMillis() - start);
        taskTimekeeper.get(measurement).remove(key);

        if (measurement.equals("process")) {
            completedCount += 1;
            queueCount -= 1;

            if (completedCount % 10 == 0) {
                queueRecord.put(System.currentTimeMillis(), queueCount);
            }
        }
    }

    public synchronized void report() {
        if (printed) {
            return;
        }

        printed = true;
        var output = new StringBuilder();
        output.append(Printer.titleString(label + " Benchmark", 30));
        output.append("\n");

        for (var entry : unitDurations.entrySet()) {
            output.append(entry.getKey());
            output.append(" : ");
            output.append(entry.getValue());
            output.append(" ms\n");
        }

        output.append("\n");

        for (var measurement : transactionDurations.keySet()) {

        }

        for (var measurement : taskDurations.keySet()) {
            output.append("[");
            output.append(measurement);
            output.append("]\n");

            var count = taskDurations.get(measurement).size();
            output.append("Count: ");
            output.append(count);

            var totalDurations = 0L;
            var minDuration = Long.MAX_VALUE;
            var maxDuration = Long.MIN_VALUE;

            for (var value : taskDurations.get(measurement).values()) {
                totalDurations += value;
                minDuration = Math.min(minDuration, value);
                maxDuration = Math.max(maxDuration, value);
            }

            output.append(", Average: ");
            output.append(totalDurations / count);
            output.append(" ms, Min: ");
            output.append(minDuration);
            output.append(" ms, Max: ");
            output.append(maxDuration);
            output.append(" ms\n\n");
        }

        if (queueRecord.size() >= 2) {
            output.append("[Throughput and Queue]\n");
            Long firstTime = null;
            var recordCount = 0;

            for (var entry : queueRecord.entrySet()) {

                var passedStr = "0.00";
                double throughput = 0;

                if (firstTime == null) {
                    firstTime = entry.getKey();
                } else {
                    var currentTime = entry.getKey();
                    var passedTime = (currentTime - firstTime) / 1000.0;
                    passedStr = (((int) (passedTime * 100)) / 100.0) + "";
                    throughput = recordCount / (passedTime);
                    throughput = ((int) (throughput * 100)) / 100.0;
                }

                output.append(passedStr);
                output.append(": throughput=");
                output.append(throughput);
                output.append(", queue=");
                output.append(entry.getValue());
                output.append("\n");

                recordCount += 10;
            }
        }

        System.out.println(output);
    }
}
