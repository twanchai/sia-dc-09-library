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
import ch.swissdotnet.siadc09.log.ByteInterceptor;
import ch.swissdotnet.siadc09.log.MessageInterceptor;
import ch.swissdotnet.siadc09.net.FrameUnwrapper;
import ch.swissdotnet.siadc09.net.FrameUnwrapperHandler;
import ch.swissdotnet.siadc09.net.MessageDecoder;
import ch.swissdotnet.siadc09.net.MessageEncoder;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.server.RctDc09;
import ch.swissdotnet.siadc09.server.listeners.ServerMessageListener;
import ch.swissdotnet.siadc09.udp.UdpDatagramDecoder;
import ch.swissdotnet.siadc09.udp.UdpDatagramEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import static ch.swissdotnet.siadc09.ChannelUtilities.*;

/**
 * The {@code UdpChannelInitializer} class manages UDP {@code Channel}s pipeline initialization.
 * <p/>
 * The pipeline consists of the following incoming stages:
 * <ol>
 * <li>UDP datagram extraction: {@code UdpDatagramDecoder}</li>
 * <li>bytes transformation: {@code MessageDecoder}</li>
 * <li>message handling: {@code MessageHandler}</li>
 * </ol>
 * The outgoing pipeline is:
 * <ol>
 * <li>bytes transformation: {@code MessageEncoder}</li>
 * <li>UDP datagram sending: {@code UdpDatagramEncoder}</li>
 * </ol>
 * Both pipelines are surrounded with {@code ExceptionHandler} to handle all {@code Exception} thrown during
 * communication.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public final class UdpChannelInitializer extends ChannelInitializer<DatagramChannel> {

    // Server parameter (whether to log bytes and/or messages or not at initialization).
    private final TransportParameters transportParameters;
    // The exception handler.
    private final ExceptionHandler exceptionHandler;
    // The byte interceptor.
    private final ByteInterceptor byteInterceptor;
    // The message interceptor.
    private final MessageInterceptor messageInterceptor;
    // The UDP datagram encoder.
    private final UdpDatagramEncoder udpDatagramEncoder;
    // The UDP datagram decoder.
    private final UdpDatagramDecoder udpDatagramDecoder;
    // The DC-09 message encoder.
    private final MessageEncoder messageEncoder;
    // The DC-09 message decoder.
    private final MessageDecoder messageDecoder;
    // The DC-09 message handler.
    private final MessageHandler messageHandler;
    // Optional pre-decoder frame unwrapper (e.g. DSC binary wrapper).
    private final FrameUnwrapperHandler frameUnwrapperHandler;

    /**
     * Initializes a new {@code UdpChannelInitializer} with given parameters.
     *
     * @param globalParameters    the DC-09 global parameters
     * @param rct                 RCT on which the message is received
     * @param store               the stored used to retrieve SPT
     * @param transportParameters server parameters whether to log or not bytes and messages
     * @param onMessageExecutor   the executor on which onMessage are sent
     * @param listeners           DC-09 listeners to call upon event
     * @param byteInterceptor     the byte logger which intercepts incoming and outgoing bytes
     * @param messageInterceptor  the message logger which intercepts incoming and outgoing messages
     */
    public UdpChannelInitializer(final Dc09GlobalParameters globalParameters,
                                 final RctDc09 rct,
                                 final Dc09SptStore store,
                                 final TransportParameters transportParameters,
                                 final ExecutorService onMessageExecutor,
                                 final Set<ServerMessageListener> listeners,
                                 final ByteInterceptor byteInterceptor,
                                 final MessageInterceptor messageInterceptor,
                                 final FrameUnwrapper frameUnwrapper) {
        this.transportParameters = transportParameters;
        this.byteInterceptor = byteInterceptor;
        this.messageInterceptor = messageInterceptor;
        Dc09Handler handler = new Dc09Handler();
        exceptionHandler = new ExceptionHandler(listeners);
        udpDatagramEncoder = new UdpDatagramEncoder();
        udpDatagramDecoder = new UdpDatagramDecoder();
        messageEncoder = new MessageEncoder(handler, globalParameters);
        messageDecoder = new MessageDecoder(handler, globalParameters, store);
        messageHandler = new MessageHandler(store, rct, onMessageExecutor, listeners, Dc09Channel.UdpChannel::new);
        frameUnwrapperHandler = frameUnwrapper != null ? new FrameUnwrapperHandler(frameUnwrapper) : null;
    }

    @Override
    protected void initChannel(final DatagramChannel ch) {

        ChannelPipeline pipeline = ch.pipeline()
            .addLast(FIRST_EXCEPTION_HANDLER, exceptionHandler);

        pipeline
            .addLast(TRANSPORT_ENCODER, udpDatagramEncoder)
            .addLast(TRANSPORT_DECODER, udpDatagramDecoder);

        if (transportParameters.isLogBytes()) {
            pipeline.addLast(BYTE_LOGGER, byteInterceptor);
        }

        if (frameUnwrapperHandler != null) {
            pipeline.addLast(FRAME_UNWRAPPER, frameUnwrapperHandler);
        }

        pipeline.addLast(BYTE_ENCODER, messageEncoder)
            .addLast(BYTE_DECODER, messageDecoder);

        if (transportParameters.isLogMessages()) {
            pipeline.addLast(MESSAGE_LOGGER, messageInterceptor);
        }

        pipeline.addLast(MESSAGE_HANDLER, messageHandler)
            .addLast(LAST_EXCEPTION_HANDLER, exceptionHandler);

    }

}
