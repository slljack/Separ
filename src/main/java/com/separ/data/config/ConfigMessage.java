package com.separ.data.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.separ.data.MessageInterface;

public class ConfigMessage implements MessageInterface {

    private Map<String, JsonNode> configMap = new HashMap<String, JsonNode>();

    public ConfigMessage() {

    }

    public ConfigMessage(Map<String, JsonNode> configMap) {
        this.configMap = configMap;
    }

    public Map<String, JsonNode> getConfigMap() {
        return configMap;
    }

    public void setConfigMap(Map<String, JsonNode> configMap) {
        this.configMap = configMap;
    }

    @Override
    public byte[] toByteArray() {
        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeInt(configMap.size());
            for (var key : configMap.keySet()) {
                outstream.writeUTF(key);
                outstream.writeUTF(configMap.get(key).toString());
            }

            return bytestream.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void fromByteArray(byte[] data) {
        var bytestream = new ByteArrayInputStream(data);
        var instream = new DataInputStream(bytestream);
        try {
            var mapSize = instream.readInt();
            configMap = new HashMap<String, JsonNode>();

            JsonFactory f = JsonFactory.builder().build();
            ObjectMapper mapper = JsonMapper.builder(f).build();

            for (var i = 0; i < mapSize; i++) {
                var key = instream.readUTF();
                var jsonStr = instream.readUTF();

                try {
                    configMap.put(key, mapper.readTree(jsonStr));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
