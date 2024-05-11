package com.separ.utils;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.separ.data.MessageInterface;
import com.separ.data.processing.MessageDecoder;
import com.separ.data.processing.MessageEncoder;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;

public class NetUtils {

    public static ChannelInitializer<SocketChannel> nettyGetInbound(Consumer<MessageInterface> messageConsumer) {
        return nettyGet(() -> new NettyInboundHandler(messageConsumer));
    }

    @SafeVarargs
    public static ChannelInitializer<SocketChannel> nettyGet(Supplier<ChannelHandler>... suppliers) {
        return new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new MessageDecoder());
                ch.pipeline().addLast(new MessageEncoder());

                if (suppliers != null && suppliers.length > 0) {
                    for (var supplier : suppliers) {
                        ch.pipeline().addLast(supplier.get());
                    }
                }
            }
        };
    }

    private static class NettyInboundHandler extends SimpleChannelInboundHandler<MessageInterface> {
        private Consumer<MessageInterface> messageConsumer;

        private NettyInboundHandler(Consumer<MessageInterface> messageConsumer) {
            this.messageConsumer = messageConsumer;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MessageInterface msg) throws Exception {
            messageConsumer.accept(msg);
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

    public static void write(Channel channel, Object message) {
        var cf = channel.write(message);
        channel.flush();
        try {
            if (!cf.await().isSuccess()) {
                System.out.println("Send failed: " + cf.cause());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void write(ChannelGroup channel, Object message) {
        var cf = channel.write(message);
        channel.flush();
        try {
            if (!cf.await().isSuccess()) {
                System.out.println("Send failed: " + cf.cause());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
