package com.separ.data.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.separ.data.MessageInterface;
import com.separ.entity.EntityType;
import com.separ.token.Constraint;

public class ConstraintMessage implements MessageInterface {
    private List<Constraint> constraints;

    public ConstraintMessage() {
        constraints = new ArrayList<Constraint>();
    }

    public ConstraintMessage(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    @Override
    public byte[] toByteArray() {
        var bytestream = new ByteArrayOutputStream();
        var outstream = new DataOutputStream(bytestream);
        try {
            outstream.writeInt(constraints.size());
            for (var constraint : constraints) {
                outstream.writeInt(constraint.getId());
                outstream.writeInt(constraint.getThreshold());
                var targets = constraint.getTargets();
                outstream.writeInt(targets.size());
                for (var target : targets) {
                    outstream.writeInt(target.ordinal());
                }
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
            var constraintCount = instream.readInt();
            constraints = new ArrayList<Constraint>();
            for (var i = 0; i < constraintCount; i++) {
                var id = instream.readInt();
                var threshold = instream.readInt();
                var targetCount = instream.readInt();
                var targets = new ArrayList<EntityType>();
                for (var j = 0; j < targetCount; j++) {
                    targets.add(EntityType.values()[instream.readInt()]);
                }
                constraints.add(new Constraint(id, targets, threshold));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
