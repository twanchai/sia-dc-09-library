/*
 * Copyright (c) 2025 Swissdotnet SA
 *
 * This file is part of the SIA-DC-09 Library project.
 *
 * This source code is dual-licensed:
 * 1. Non-commercial use is permitted under the Polyform Noncommercial License 1.0.0
 *    https://polyformproject.org/licenses/noncommercial/1.0.0/
 * 2. Commercial use requires a separate commercial license.
 *    To inquire about licensing, please contact: info@swissdotnet.ch
 *
 * Unless required by applicable law or agreed to in writing, this software
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 */
package ch.swissdotnet.siadc09.server.impl;

import ch.swissdotnet.siadc09.Dc09SptStore;
import ch.swissdotnet.siadc09.TransportParameters;
import ch.swissdotnet.siadc09.log.ByteInterceptor;
import ch.swissdotnet.siadc09.log.MessageInterceptor;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.server.RctDc09;
import ch.swissdotnet.siadc09.server.SiaDc09Server;
import ch.swissdotnet.siadc09.tcp.ChannelManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static ch.swissdotnet.siadc09.server.SiaDc09Servers.SiaThreadFactory;

/**
 * The {@code TcpSiaDc09Server} class manages TCP connection for SIA DC-09 SPTs.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 * @see TcpChannelInitializer
 */
public final class TcpSiaDc09Server extends AbstractServer {

    // SLF4J Logger
    private static final Logger LOG = LoggerFactory.getLogger(TcpSiaDc09Server.class);

    // The boss event loop group.
    private final EventLoopGroup bossGroup;
    // The worker event loop group.
    private final EventLoopGroup workerGroup;
    // The TCP channel initializer.
    private final TcpChannelInitializer channelInitializer;
    // The TCP channels manager.
    private final ChannelManager channelManager;

    /**
     * Initializes a new {@code TcpSiaDc09Server} with given parameters.
     *
     * @param rct                 the server representation as RCT
     * @param transportParameters the server parameters
     * @param onMessageExecutor   the onMessage executor
     * @param globalParameters    the DC-09 global parameters
     * @param store               the DC-09 SPT store
     */
    public TcpSiaDc09Server(final RctDc09 rct,
                            final TransportParameters transportParameters,
                            final ExecutorService onMessageExecutor,
                            final Dc09GlobalParameters globalParameters,
                            final Dc09SptStore store) {

        super(rct, transportParameters, onMessageExecutor);
        bossGroup = new NioEventLoopGroup(1, new SiaThreadFactory("SIA-DC09-TCP-Server-BG"));
        workerGroup = new NioEventLoopGroup(transportParameters.getChannelThread(), new SiaThreadFactory("SIA-DC09-TCP-Server-WG"));

        ByteInterceptor byteInterceptor = new ByteInterceptor(listeners);
        MessageInterceptor messageInterceptor = new MessageInterceptor(store, listeners);
        channelManager = new ChannelManager(byteInterceptor, messageInterceptor);

        channelInitializer = new TcpChannelInitializer(
            globalParameters,
            rct,
            store,
            transportParameters,
            this.onMessageExecutor,
            listeners,
            channelManager
        );
    }

    @Override
    public void run() {

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(channelInitializer)
            .option(ChannelOption.SO_REUSEADDR, rct.getTransport() == RctDc09.Transport.BOTH)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            channels.add(bootstrap.bind(rct.getIp(), rct.getPort()).sync().channel());
            for (Channel channel : channels) {
                channel.closeFuture().await();
            }
        } catch (InterruptedException e) {
            LOG.error("Error while awaiting all TCP SIA-DC09 servers ({}).", e.getMessage(), e);
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public SiaDc09Server setLogBytes(final boolean flag) {
        super.setLogBytes(flag);
        channelManager.logBytes(flag);
        return this;
    }

    @Override
    public SiaDc09Server setLogMessages(final boolean flag) {
        super.setLogMessages(flag);
        channelManager.logMessages(flag);
        return this;
    }

}
