package com.separ.data.processing;

import java.io.IOException;
import java.util.List;

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
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

public class MessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // Wait until the length prefix is available.
        if (in.readableBytes() < 5) {
            return;
        }

        in.markReaderIndex();

        // Config, Constraints, Crypto Keys, Entity Infos, Tokens, Entity Transactions, Node Transactions
        var validBytes = List.of('C', 'O', 'R', 'E', 'T', 'N', 'D', 'L');

        // Check the magic number.
        char magicNumber = (char) in.readUnsignedByte();
        if (!validBytes.contains(magicNumber)) {
            in.resetReaderIndex();
            throw new CorruptedFrameException("Invalid magic number: " + magicNumber);
        }

        // Wait until the whole data is available.
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }

        var data = new byte[dataLength];
        in.readBytes(data);

        MessageInterface msg = null;
        if (magicNumber == 'C') {
            msg = new ConfigMessage();
        } else if (magicNumber == 'O') {
            msg = new ConstraintMessage();
        } else if (magicNumber == 'R') {
            msg = new CryptoMessage();
        } else if (magicNumber == 'E') {
            msg = new EntityInfoMessage();
        } else if (magicNumber == 'T') {
            msg = new TokenMessage();
        } else if (magicNumber == 'N') {
            msg = new EntityTransactionMessage();
        } else if (magicNumber == 'D') {
            msg = new NodeTransactionMessage();
        } else if (magicNumber == 'L') {
            msg = new ControlMessage();
        }

        msg.fromByteArray(data);
        out.add(msg);
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
