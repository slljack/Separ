package com.separ.data.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.separ.data.MessageInterface;
import com.separ.entity.EntityInfo;
import com.separ.entity.EntityType;

public class EntityInfoMessage implements MessageInterface {
    private List<EntityInfo> entities;

    public EntityInfoMessage() {
        entities = new ArrayList<EntityInfo>();
    }

    public EntityInfoMessage(List<EntityInfo> entities) {
        this.entities = entities;
    }

    public List<EntityInfo> getEntities() {
        return entities;
    }

    @Override
    public byte[] toByteArray() {
        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeInt(entities.size());
            for (var entity : entities) {
                outstream.writeInt(entity.type().ordinal());
                outstream.writeInt(entity.getId());
                outstream.writeUTF(entity.getIp());
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
            var entityCount = instream.readInt();
            entities = new ArrayList<EntityInfo>();
            for (var i = 0; i < entityCount; i++) {
                var entityType = EntityType.values()[instream.readInt()];
                var id = instream.readInt();
                var ip = instream.readUTF();
                entities.add(new EntityInfo(entityType, id, ip));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
