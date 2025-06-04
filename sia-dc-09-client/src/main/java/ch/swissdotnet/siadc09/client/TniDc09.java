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
package ch.swissdotnet.siadc09.client;

import ch.swissdotnet.siadc09.*;
import ch.swissdotnet.siadc09.client.impl.ClientExceptionHandler;
import ch.swissdotnet.siadc09.client.impl.MessageHandler;
import ch.swissdotnet.siadc09.client.impl.SptStore;
import ch.swissdotnet.siadc09.client.impl.TcpUdpDc09Client;
import ch.swissdotnet.siadc09.holders.MessageOutboundHolder;
import ch.swissdotnet.siadc09.messages.versions.StandardDc09;
import ch.swissdotnet.siadc09.net.MessageDecoder;
import ch.swissdotnet.siadc09.net.MessageEncoder;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.tcp.TcpPacketDecoder;
import ch.swissdotnet.siadc09.tcp.TcpPacketEncoder;
import ch.swissdotnet.siadc09.udp.UdpDatagramDecoder;
import ch.swissdotnet.siadc09.udp.UdpDatagramEncoder;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Queues;
import com.google.common.net.HostAndPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.swissdotnet.siadc09.ChannelUtilities.*;
import static com.google.common.base.Preconditions.*;


public abstract class TniDc09 implements AutoCloseable {

    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger(1);
    private final HostAndPort hostAndPort;
    private final int timeout;
    private final Transport transport;
    private final SptStore store;
    private final boolean eventLoopOwnership;
    private final EventLoopGroup eventGroup;
    private final ClientExceptionHandler clientExceptionHandler;
    private final MessageEncoder messageEncoder;
    private final MessageDecoder messageDecoder;
    private final CyclicAtomicCounter sequence = new CyclicAtomicCounter(1, 9999);

    protected TniDc09(final Builder builder) {
        this.hostAndPort = builder.hostAndPort;
        this.timeout = builder.timeout;
        this.transport = builder.transport;
        this.store = new SptStore();
        this.eventLoopOwnership = builder.eventLoopGroup == null;
        if (eventLoopOwnership) {
            this.eventGroup = new NioEventLoopGroup(builder.eventLoopGroupSize, builder.threadFactory);
        } else {
            this.eventGroup = builder.eventLoopGroup;
        }
        this.clientExceptionHandler = new ClientExceptionHandler();
        this.messageEncoder = new MessageEncoder(builder.writer, builder.globalParameters);
        this.messageDecoder = new MessageDecoder(builder.reader, builder.globalParameters, store);
    }

    public static Builder newTcpAtp(final HostAndPort hostAndPort) {
        return newAtp(hostAndPort, Transport.TCP);
    }

    public static Builder newUdpAtp(final HostAndPort hostAndPort) {
        return newAtp(hostAndPort, Transport.UDP);
    }

    public static Builder newAtp(final HostAndPort hostAndPort, final Transport transport) {
        return new Builder(hostAndPort).transport(transport);
    }

    public Optional<TniDc09Client> sender(final ClientResponseListener listener) {
        return sender(new SenderParameters(), listener);
    }

    abstract public Optional<TniDc09Client> sender(final SenderParameters senderParameters,
                                                   final ClientResponseListener listener);

    public void removeSpt(final Dc09Spt spt) {
        this.store.removeSpt(spt.getAccountNumber());
    }

    public HostAndPort getHostAndPort() {
        return hostAndPort;
    }

    public int getTimeout() {
        return timeout;
    }

    public SptStore getStore() {
        return store;
    }

    public EventLoopGroup getEventGroup() {
        return eventGroup;
    }

    public ClientExceptionHandler getClientExceptionHandler() {
        return clientExceptionHandler;
    }

    public MessageEncoder getMessageEncoder() {
        return messageEncoder;
    }

    public MessageDecoder getMessageDecoder() {
        return messageDecoder;
    }

    public CyclicAtomicCounter getSequence() {
        return sequence;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("hostAndPort", hostAndPort)
            .add("timeout", timeout)
            .add("transport", transport)
            .toString();
    }

    @Override
    public int hashCode() {return Objects.hashCode(hostAndPort, transport);}

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TniDc09 other = (TniDc09) obj;
        return Objects.equal(this.hostAndPort, other.hostAndPort)
            && Objects.equal(this.transport, other.transport);
    }

    @Override
    public void close() {
        if (this.eventLoopOwnership) {
            this.eventGroup.shutdownGracefully(0, timeout, TimeUnit.SECONDS);
        }
    }

    public enum Transport {
        /**
         * User Datagram Protocol.
         */
        UDP,
        /**
         * Transmission Control Protocol.
         */
        TCP
    }

    public static final class Builder {

        private final HostAndPort hostAndPort;
        private final Dc09Handler defaultHandler = new Dc09Handler();
        private int eventLoopGroupSize = 10;
        private EventLoopGroup eventLoopGroup;
        private ThreadFactory threadFactory;
        private Dc09GlobalParameters globalParameters = new Dc09GlobalParameters();
        private Dc09Writer writer = defaultHandler;
        private Dc09Reader reader = defaultHandler;
        private int timeout = 10;
        private Transport transport = Transport.UDP;
        private TransportParameters transportParameters = new TransportParameters();

        private Builder(final HostAndPort hostAndPort) {
            this.hostAndPort = checkNotNull(hostAndPort);
        }

        public Builder withTimeoutInSeconds(final int timeout) {
            checkArgument(timeout >= 0);
            this.timeout = timeout;
            return this;
        }

        public Builder withTransportParamters(final TransportParameters transportParameters) {
            this.transportParameters = checkNotNull(transportParameters);
            return this;
        }

        public Builder transport(final Transport transport) {
            this.transport = checkNotNull(transport);
            return this;
        }

        public Builder withDc09Parameters(final Dc09GlobalParameters globalParameters) {
            this.globalParameters = checkNotNull(globalParameters);
            return this;
        }

        public Builder withWriter(final Dc09Writer writer) {
            this.writer = checkNotNull(writer);
            return this;
        }

        public Builder withReader(final Dc09Reader reader) {
            this.reader = checkNotNull(reader);
            return this;
        }

        public Builder withThreadFactory(final ThreadFactory threadFactory) {
            this.threadFactory = checkNotNull(threadFactory);
            return this;
        }

        public Builder withEventLoopGroupSize(final int eventLoopGroupSize) {
            checkArgument(eventLoopGroupSize > 0);
            this.eventLoopGroupSize = eventLoopGroupSize;
            return this;
        }

        public Builder withEventLoopGroup(final EventLoopGroup eventLoopGroup) {
            this.eventLoopGroup = checkNotNull(eventLoopGroup);
            return this;
        }

        public TniDc09 build() {
            if (threadFactory == null) {
                threadFactory = r -> new Thread(r, "SIA-DC09-" + transport + "-Client-" + THREAD_SEQUENCE.incrementAndGet());
            }
            switch (transport) {
                case TCP: {
                    return new TcpTniDc09(this);
                }
                default:
                case UDP: {
                    return new UdpTniDc09(this);
                }
            }
        }

    }

    private static class TcpTniDc09 extends TniDc09 {

        private final Bootstrap bootstrap;
        private final StandardDc09 STANDARD_DC_09 = new StandardDc09();
        private final TcpPacketEncoder encoder = new TcpPacketEncoder();

        private TcpTniDc09(final Builder builder) {
            super(builder);
            this.bootstrap = new Bootstrap()
                .group(super.eventGroup)
                .remoteAddress(super.hostAndPort.getHost(), super.hostAndPort.getPort())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(super.timeout))
                .handler(
                    new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(final NioSocketChannel channel) {
                            channel.pipeline()
                                .addLast(FIRST_EXCEPTION_HANDLER, TcpTniDc09.super.clientExceptionHandler)
                                .addLast(TRANSPORT_ENCODER, encoder)
                                .addLast(TRANSPORT_DECODER, new TcpPacketDecoder(STANDARD_DC_09.maxMessageLength()))
                                .addLast(BYTE_DECODER, TcpTniDc09.super.messageDecoder)
                                .addLast(BYTE_ENCODER, TcpTniDc09.super.messageEncoder)
                                .addLast(LAST_EXCEPTION_HANDLER, TcpTniDc09.super.clientExceptionHandler);

                        }
                    }
                );
        }

        @Override
        public Optional<TniDc09Client> sender(final SenderParameters senderParameters,
                                              final ClientResponseListener listener) {
            try {
                ChannelFuture connection = bootstrap.connect().syncUninterruptibly();

                if (!connection.isSuccess()) {
                    return Optional.empty();
                }

                Queue<MessageOutboundHolder> messages = Queues.newConcurrentLinkedQueue();
                ChannelDuplexHandler handler = new MessageHandler(messages, super.timeout, listener);
                ChannelPipeline pipeline = connection
                    .channel()
                    .pipeline();

                pipeline
                    .addAfter(BYTE_ENCODER, MESSAGE_HANDLER, handler)
                    .addAfter(
                        MESSAGE_HANDLER, "TCP_DISCONNECT", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
                                super.channelUnregistered(ctx);
                                ctx.disconnect();
                                listener.onDisconnect(TcpTniDc09.this);
                            }
                        }
                    );
                return Optional.of(new TcpUdpDc09Client(senderParameters, getStore(), connection.channel(), super.sequence));

            } catch (final Throwable t) {
                return Optional.empty();
            }
        }
    }

    private static class UdpTniDc09 extends TniDc09 {

        private final Bootstrap bootstrap;
        private final UdpDatagramEncoder encoder = new UdpDatagramEncoder();

        private UdpTniDc09(final Builder builder) {
            super(builder);
            this.bootstrap = new Bootstrap()
                .group(super.eventGroup)
                .remoteAddress(super.hostAndPort.getHost(), super.hostAndPort.getPort())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(super.timeout))
                .channel(NioDatagramChannel.class)
                .handler(
                    new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(final NioDatagramChannel channel) {
                            channel.pipeline()
                                .addLast(FIRST_EXCEPTION_HANDLER, UdpTniDc09.super.clientExceptionHandler)
                                .addLast(TRANSPORT_ENCODER, encoder)
                                .addLast(TRANSPORT_DECODER, new UdpDatagramDecoder())
                                .addLast(BYTE_DECODER, UdpTniDc09.super.messageDecoder)
                                .addLast(BYTE_ENCODER, UdpTniDc09.super.messageEncoder)
                                .addLast(LAST_EXCEPTION_HANDLER, UdpTniDc09.super.clientExceptionHandler);
                        }
                    }
                )
            ;
        }

        @Override
        public Optional<TniDc09Client> sender(final SenderParameters senderParameters,
                                              final ClientResponseListener listener) {
            try {
                ChannelFuture connection = bootstrap.connect().syncUninterruptibly();

                if (!connection.isSuccess()) {
                    return Optional.empty();
                }
                Queue<MessageOutboundHolder> messages = Queues.newConcurrentLinkedQueue();
                ChannelDuplexHandler handler = new MessageHandler(messages, super.timeout, listener);
                ChannelPipeline pipeline = connection
                    .syncUninterruptibly()
                    .channel()
                    .pipeline();

                pipeline
                    .addAfter(BYTE_ENCODER, MESSAGE_HANDLER, handler);
                return Optional.of(new TcpUdpDc09Client(senderParameters, getStore(), connection.channel(), super.sequence));

            } catch (final Throwable t) {
                return Optional.empty();
            }
        }
    }

}
