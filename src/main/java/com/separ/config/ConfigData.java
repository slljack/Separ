package com.separ.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class ConfigData {

    private static Map<String, JsonNode> data = new HashMap<String, JsonNode>();

    static {
        loadConfig("init");
    }

    public static void loadConfig(String name) {
        JsonFactory f = JsonFactory.builder().build();
        ObjectMapper mapper = JsonMapper.builder(f).build();
        try {
            data.put(name, mapper.readTree(new File("config/" + name + ".json")));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void putAll(Map<String, JsonNode> allConfig) {
        data.putAll(allConfig);
    }

    public static int getInt(String name) {
        return get(name).asInt();
    }

    public static String getString(String name) {
        return get(name).asText();
    }

    public static boolean getBoolean(String name) {return get(name).asBoolean();}

    public static List<Integer> getIntList(String name) {
        var list = new ArrayList<Integer>();
        get(name).elements().forEachRemaining(x -> list.add(x.asInt()));
        return list;
    }

    public static JsonNode get(String name) {
        var parts = name.split("\\.", 2);
        if (parts.length < 1 || !data.containsKey(parts[0])) {
            throw new RuntimeException("Bad config key: " + parts[0]);
        }

        var configNode = data.get(parts[0]);
        for (var field : parts[1].split("\\.")) {
            var index = getNumeric(field);
            configNode = index > -1 ? configNode.get(index) : configNode.get(field);
        }

        return configNode;
    }

    private static int getNumeric(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static Map<String, JsonNode> all() {
        return data;
    }
}
