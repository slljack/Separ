package com.separ.utils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.separ.config.ConfigData;
import com.separ.entity.EntityType;

public class UrlHelper {

    public static int getEntityPort(EntityType type, int id) {

        var offset = ConfigData.getInt("init.EntityPorts");
        return offset + type.ordinal() * 500 + id * 5;
    }

    public static int getPacekeeperPort(EntityType type, int id) {
        return getEntityPort(type, id) + 1;
    }

    public static URL getUrl(String ip, int port, String path) {
        try {
            return new URL("http://" + ip + ":" + port + "/" + path);
        } catch (java.net.MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String readContent(URL targetUrl) {
        String content = null;
        try (var stream = targetUrl.openStream()) {
            var bytes = stream.readAllBytes();
            content = new String(bytes, StandardCharsets.UTF_8);
            stream.close();
        } catch (ConnectException e) {
            System.err.println("Could not connect to " + targetUrl + ".");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }
}
