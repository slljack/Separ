package com.separ.flow;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.separ.config.ConfigData;
import com.separ.entity.EntityInfo;
import com.separ.entity.EntityManager;
import com.separ.entity.EntityType;
import com.separ.flow.StateTracker.StateEvent;
import com.separ.utils.UrlHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Pacemaker {

    private HttpServer server;

    public Pacemaker() {

    }

    public void run() {
        try {
            var port = ConfigData.getInt("init.PacemakerPort");
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new RequestHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("Pacemaker stared.");
            System.out.println("Connected entities:");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void call(EntityType entityType, int entityId, String action) {
        var entityInfo = EntityManager.get(entityType, entityId);
        var ip = entityInfo.getIp();
        var port = UrlHelper.getPacekeeperPort(entityType, entityId);
        var actionUrl = UrlHelper.getUrl(ip, port, action);
        UrlHelper.readContent(actionUrl);
    }

    public void stop() {
        server.stop(2);
    }

    private class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            var action = exchange.getRequestURI().getPath();
            action = action.startsWith("/") ? action.substring(1) : action;
            var parts = action.split("/");
            var entityType = EntityType.get(parts[0]);
            var entityId = Integer.parseInt(parts[1]);
            var state = parts[2];

            var ip = exchange.getRemoteAddress().getHostName();

            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            OutputStream responseStream = exchange.getResponseBody();
            responseStream.close();

            if (state.equals("init")) {
                System.out.println(" - " + entityType.id() + " #" + entityId);
            }

            // System.out.println("[Pacemaker] Called " + entityType.id() + " #" + entityId + " " + state);

            String oldState = EntityManager.getState(entityType, entityId);
            var entity = new EntityInfo(entityType, entityId, ip);
            if (oldState != state) {
                StateTracker.fireStateChanged(new StateEvent(entity, oldState, state));
            }
        }
    }
}
