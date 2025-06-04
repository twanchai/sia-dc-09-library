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
import ch.swissdotnet.siadc09.server.SiaDc09Servers;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.LockSupport;

import static ch.swissdotnet.siadc09.ChannelUtilities.setLogBytesOnChannel;
import static ch.swissdotnet.siadc09.ChannelUtilities.setLogMessagesOnChannel;

/**
 * The {@code UdpSiaDc09Server} class manages UDP connection for SIA DC-09 SPTs.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 * @see UdpChannelInitializer
 */
public final class UdpSiaDc09Server extends AbstractServer {

    // SLF4J Logger
    private static final Logger LOG = LoggerFactory.getLogger(UdpSiaDc09Server.class);

    // The working group.
    private final UdpChannelInitializer channelInitializer;
    // The UDP channel initializer.
    private final ByteInterceptor byteInterceptor;
    // The byte interceptor used to log IN/OUT bytes.
    private final MessageInterceptor messageInterceptor;
    // The transport parameters.
    private final TransportParameters transportParameters;

    /**
     * Initializes a new {@code UdpSiaDc09Server} with given parameters.
     *
     * @param rct                 the server representation as RCT
     * @param transportParameters the server parameters
     * @param onMessageExecutor   the onMessage executor
     * @param globalParameters    the DC-09 global parameters
     * @param store               the DC-09 SPT store
     */
    public UdpSiaDc09Server(final RctDc09 rct,
                            final TransportParameters transportParameters,
                            final ExecutorService onMessageExecutor,
                            final Dc09GlobalParameters globalParameters,
                            final Dc09SptStore store) {
        super(rct, transportParameters, onMessageExecutor);
        this.byteInterceptor = new ByteInterceptor(listeners);
        this.messageInterceptor = new MessageInterceptor(store, listeners);
        this.transportParameters = transportParameters;
        channelInitializer = new UdpChannelInitializer(
            globalParameters,
            rct,
            store,
            this.transportParameters,
            this.onMessageExecutor,
            listeners,
            byteInterceptor,
            messageInterceptor
        );
    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {
            EventLoopGroup group = new NioEventLoopGroup(
                transportParameters.getChannelThread(),
                new SiaDc09Servers.SiaThreadFactory("SIA-DC09-UDP-Server")
            );
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                    .option(ChannelOption.SO_REUSEADDR, rct.getTransport() == RctDc09.Transport.BOTH)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(channelInitializer)
                    .channel(NioDatagramChannel.class);
                channels.add(bootstrap.bind(rct.getIp(), rct.getPort()).sync().channel());
                for (Channel channel : channels) {
                    channel.closeFuture().await();
                }
                LOG.info("Interrupted UDP servers.");
            } catch (Throwable e) {
                LOG.error("Error while starting/awaiting all UDP SIA-DC09 servers ({}).", e.getMessage(), e);
            } finally {
                group.shutdownGracefully();
            }
            if (running.get()) {
                LOG.info("Awaiting before restarting UDP channel.");
                LockSupport.parkNanos(transportParameters.getAwaitAfterChannelInterruptedNs());
            }
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        for (Channel channel : channels) {
            channel.close();
        }
    }

    @Override
    public SiaDc09Server setLogBytes(final boolean flag) {
        super.setLogBytes(flag);
        for (Channel channel : channels) {
            setLogBytesOnChannel(flag, channel, byteInterceptor);
        }
        return this;
    }

    @Override
    public SiaDc09Server setLogMessages(final boolean flag) {
        super.setLogMessages(flag);
        for (Channel channel : channels) {
            setLogMessagesOnChannel(flag, channel, messageInterceptor);
        }
        return this;
    }

}
