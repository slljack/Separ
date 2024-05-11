package com.separ.utils;

import java.util.HashMap;

import com.separ.entity.EntityType;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ChannelManager {
    private HashMap<String, Channel> inboundChannels;
    private HashMap<String, Channel> outboundChannels;
    private HashMap<String, ChannelGroup> outboundGroups;

    // TODO: Define as static to share between Nodes ?
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public ChannelManager() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);

        inboundChannels = new HashMap<String, Channel>();
        outboundChannels = new HashMap<String, Channel>();
        outboundGroups = new HashMap<String, ChannelGroup>();

        for (var entityType : EntityType.values()) {
            outboundGroups.put(entityType.id(), new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
        }

        outboundGroups.put("all", new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
    }

    public void addInboundChannel(String channelKey, ChannelInitializer<SocketChannel> initializer, int port)
            throws InterruptedException {

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
        b.option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
        b.childHandler(initializer);

        // Bind and start to accept incoming connections.
        var channelFuture = b.bind(port).sync();
        var inboundChannel = channelFuture.channel();
        inboundChannels.put(channelKey, inboundChannel);
    }

    public void addOutboundChannel(String channelId, String ip, int port,
            ChannelInitializer<SocketChannel> initializer) {
        var channel = createOutboundChannel(ip, port, initializer);
        if (channel != null) {
            outboundChannels.put(channelId, channel);
        }
    }

    public void addOutboundChannel(String groupId, String channelId, String ip, int port,
            ChannelInitializer<SocketChannel> initializer) {
        var channel = createOutboundChannel(ip, port, initializer);
        if (channel == null) {
            return;
        }

        outboundChannels.put(channelId, channel);

        if (!outboundGroups.containsKey(groupId)) {
            outboundGroups.put(groupId, new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
        }

        outboundGroups.get(groupId).add(channel);
        outboundGroups.get("all").add(channel);
    }

    public Channel getOutboundChannel(String key) {
        return outboundChannels.get(key);
    }

    public ChannelGroup getOutboundGroup(String key) {
        return outboundGroups.get(key);
    }

    private Channel createOutboundChannel(String ip, int port, ChannelInitializer<SocketChannel> initializer) {
        Bootstrap b = new Bootstrap();
        b.group(workerGroup).channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(initializer);

        // Start the client.
        ChannelFuture channelFuture;
        try {
            channelFuture = b.connect(ip, port).sync();
            return channelFuture.channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void shutdown() {
        for (var group : outboundGroups.values()) {
            group.close();
        }

        for (var channel : outboundChannels.values()) {
            if (channel.isOpen()) {
                channel.close();
            }
        }

        for (var channel : inboundChannels.values()) {
            channel.close();
        }

        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}