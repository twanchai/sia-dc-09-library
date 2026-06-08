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

import ch.swissdotnet.siadc09.Dc09Handler;
import ch.swissdotnet.siadc09.Dc09SptStore;
import ch.swissdotnet.siadc09.ExceptionHandler;
import ch.swissdotnet.siadc09.TransportParameters;
import ch.swissdotnet.siadc09.messages.versions.StandardDc09;
import ch.swissdotnet.siadc09.net.FrameUnwrapper;
import ch.swissdotnet.siadc09.net.FrameUnwrapperHandler;
import ch.swissdotnet.siadc09.net.MessageDecoder;
import ch.swissdotnet.siadc09.net.MessageEncoder;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.server.RctDc09;
import ch.swissdotnet.siadc09.server.listeners.ServerMessageListener;
import ch.swissdotnet.siadc09.tcp.ChannelManager;
import ch.swissdotnet.siadc09.tcp.TcpPacketDecoder;
import ch.swissdotnet.siadc09.tcp.TcpPacketEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import static ch.swissdotnet.siadc09.ChannelUtilities.*;

/**
 * The {@code TcpChannelInitializer} class manages TCP {@code Channel}s pipeline initialization.
 * <p/>
 * The pipeline consists of the following incoming stages:
 * <ol>
 * <li>TCP stream byte slice: {@code TcpPacketDecoder}</li>
 * <li>bytes transformation: {@code MessageDecoder}</li>
 * <li>message handling: {@code MessageHandler}</li>
 * </ol>
 * The outgoing pipeline is:
 * <ol>
 * <li>bytes transformation: {@code MessageEncoder}</li>
 * <li>TCP stream sending: {@code TcpPacketEncoder}</li>
 * </ol>
 * Both pipelines are surrounded with {@code ExceptionHandler} to handle all {@code Exception} thrown during
 * communication.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public final class TcpChannelInitializer extends ChannelInitializer<SocketChannel> {

    // Reference to the maximum CRC size and maximum message length used to split TCP stream.
    private static final StandardDc09 STANDARD_DC_09 = new StandardDc09();
    // Server parameter (whether to log bytes and/or messages or not at initialization).
    private final TransportParameters transportParameters;
    // Channel manager to add/remove byte and message interceptor.
    private final ChannelManager channelManager;
    // The exception handler.
    private final ExceptionHandler exceptionHandler;
    // The TCP packet encoder.
    private final TcpPacketEncoder tcpPacketEncoder;
    // The DC-09 message encoder.
    private final MessageEncoder messageEncoder;
    // The DC-09 message decoder.
    private final MessageDecoder messageDecoder;
    // The DC-09 message handler.
    private final MessageHandler messageHandler;
    // Optional pre-decoder frame unwrapper (e.g. DSC binary wrapper).
    private final FrameUnwrapperHandler frameUnwrapperHandler;

    /**
     * Initializes a new {@code TcpChannelInitializer} with given parameters.
     *
     * @param globalParameters    the DC-09 global parameters
     * @param rct                 RCT on which the message is received
     * @param store               the stored used to retrieve SPT
     * @param onMessageExecutor   the executor on which onMessage are sent
     * @param transportParameters server parameters whether to log or not bytes and messages
     * @param listeners           DC-09 listeners to call upon event
     * @param channelManager      the byte and message logger manager
     */
    public TcpChannelInitializer(final Dc09GlobalParameters globalParameters,
                                 final RctDc09 rct,
                                 final Dc09SptStore store,
                                 final TransportParameters transportParameters,
                                 final ExecutorService onMessageExecutor,
                                 final Set<ServerMessageListener> listeners,
                                 final ChannelManager channelManager,
                                 final FrameUnwrapper frameUnwrapper) {
        this.transportParameters = transportParameters;
        this.channelManager = channelManager;
        Dc09Handler handler = new Dc09Handler();
        exceptionHandler = new ExceptionHandler(listeners);
        tcpPacketEncoder = new TcpPacketEncoder();
        messageEncoder = new MessageEncoder(handler, globalParameters);
        messageDecoder = new MessageDecoder(handler, globalParameters, store);
        messageHandler = new MessageHandler(store, rct, onMessageExecutor, listeners, Dc09Channel.TcpChannel::new);
        frameUnwrapperHandler = frameUnwrapper != null ? new FrameUnwrapperHandler(frameUnwrapper) : null;
    }

    @Override
    protected void initChannel(final SocketChannel ch) {

        ChannelPipeline pipeline = ch.pipeline()
            .addLast(FIRST_EXCEPTION_HANDLER, exceptionHandler);

        pipeline
            .addLast(CHANNEL_MANAGER, channelManager)
            .addLast(TRANSPORT_ENCODER, tcpPacketEncoder)
            .addLast(TRANSPORT_DECODER, new TcpPacketDecoder(STANDARD_DC_09.maxMessageLength()));

        if (transportParameters.isLogBytes()) {
            pipeline.addLast(BYTE_LOGGER, channelManager.getByteInterceptor());
        }

        if (frameUnwrapperHandler != null) {
            pipeline.addLast(FRAME_UNWRAPPER, frameUnwrapperHandler);
        }

        pipeline.addLast(BYTE_ENCODER, messageEncoder)
            .addLast(BYTE_DECODER, messageDecoder);

        if (transportParameters.isLogMessages()) {
            pipeline.addLast(MESSAGE_LOGGER, channelManager.getMessageInterceptor());
        }

        pipeline.addLast(MESSAGE_HANDLER, messageHandler)
            .addLast(LAST_EXCEPTION_HANDLER, exceptionHandler);

    }

}
