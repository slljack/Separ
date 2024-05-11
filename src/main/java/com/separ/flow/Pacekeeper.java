package com.separ.flow;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.separ.config.ConfigData;
import com.separ.entity.EntityBase;
import com.separ.utils.Printer;
import com.separ.utils.UrlHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Pacekeeper {

    private HttpServer server;
    private EntityBase owner;

    private Printer printer;

    public Pacekeeper(EntityBase owner) {
        this.owner = owner;
        printer = owner.getPrinter();
    }

    public void run() {
        try {
            var port = UrlHelper.getPacekeeperPort(owner.getType(), owner.getId());
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new RequestHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            printer.info("[Pacekeeper] Start init");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void call(String action) {
        var ip = ConfigData.getString("init.RegistrationAuthorityIp");
        var port = ConfigData.getInt("init.PacemakerPort");
        var actionPath = owner.getType().id() + "/" + owner.getId() + "/" + action;
        var actionUrl = UrlHelper.getUrl(ip, port, actionPath);
        UrlHelper.readContent(actionUrl);
    }

    public void stop() {
        server.stop(2);
    }

    private class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            var action = exchange.getRequestURI().getPath();
            var state = action.startsWith("/") ? action.substring(1) : action;
            if (state.equals("stop")) {
                printer.info("[Pacekeeper] Stopping...");
            } else {
                printer.info("[Pacekeeper] Start " + state);
            }
            owner.setState(state);

            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            OutputStream responseStream = exchange.getResponseBody();
            responseStream.close();
        }
    }
}
