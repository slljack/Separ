package com.separ.entity;

public class PlatformWithNodes {

    public static void main(String[] args) {
        if (args.length < 3 || args.length > 4) {
            System.out.println("Wrong parameter count.");
            return;
        }

        int platformCount, nodeCount, nodesPerPlatform, platformStartId, platformEndId;

        try {
            platformCount = Integer.parseInt(args[0]);
            nodeCount = Integer.parseInt(args[1]);
            nodesPerPlatform = nodeCount / platformCount;
            platformStartId = Integer.parseInt(args[2]);
            if (args.length == 3) {
                platformEndId = platformStartId;
            } else {
                platformEndId = Integer.parseInt(args[3]);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
            return;
        }

        for (var pid = platformStartId; pid <= platformEndId; pid++) {
            new Platform(pid, nodeCount, platformCount).start();
            for (var i = 0; i < nodesPerPlatform; i++) {
                var nid = pid * nodesPerPlatform + i;
                new Node(nid, pid).start();
            }
        }
    }
}
