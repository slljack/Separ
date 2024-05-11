package com.separ.data.processing;

import java.io.IOException;

import com.separ.data.MessageInterface;
import com.separ.data.config.ConfigMessage;
import com.separ.data.config.ConstraintMessage;
import com.separ.data.config.CryptoMessage;
import com.separ.data.config.EntityInfoMessage;
import com.separ.data.config.TokenMessage;
import com.separ.data.entity.ControlMessage;
import com.separ.data.entity.EntityTransactionMessage;
import com.separ.data.entity.NodeTransactionMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<MessageInterface> {

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageInterface msg, ByteBuf out) {
        var data = msg.toByteArray();

        if (msg instanceof ConfigMessage) {
            out.writeByte((byte) 'C');
        } else if (msg instanceof ConstraintMessage) {
            out.writeByte((byte) 'O');
        } else if (msg instanceof CryptoMessage) {
            out.writeByte((byte) 'R');
        } else if (msg instanceof EntityInfoMessage) {
            out.writeByte((byte) 'E');
        } else if (msg instanceof TokenMessage) {
            out.writeByte((byte) 'T');
        } else if (msg instanceof EntityTransactionMessage) {
            out.writeByte((byte) 'N');
        } else if (msg instanceof NodeTransactionMessage) {
            out.writeByte((byte) 'D');
        } else if (msg instanceof ControlMessage) {
            out.writeByte((byte) 'L');
        }

        out.writeInt(data.length);
        out.writeBytes(data);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            System.out.println("A netty connection was lost.");
        } else {
            cause.printStackTrace();
        }

        ctx.close();
    }
}
