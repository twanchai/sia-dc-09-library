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
package ch.swissdotnet.siadc09;

import ch.swissdotnet.siadc09.log.ByteInterceptor;
import ch.swissdotnet.siadc09.log.MessageInterceptor;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * The {@code ChannelUtilities} class offers Netty utilities for {@code Channel}s.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public final class ChannelUtilities {

    /**
     * SPT Attribute Key to store the concerned SPT on channel.
     */
    public static final AttributeKey<Dc09Spt> SPT = AttributeKey.valueOf("SPT_DC_09");
    /**
     * SPT Attribute Key to store the concerned SPT on channel.
     */
    public static final AttributeKey<RemoteAddressResolver> REMOTE_ADDRESS_RESOLVER = AttributeKey.valueOf("REMOTE_ADDRESS_RESOLVER");
    /**
     * Exception handler (first position).
     */
    public static final String FIRST_EXCEPTION_HANDLER = "firstExceptionHandler";
    /**
     * Exception handler (last position).
     */
    public static final String LAST_EXCEPTION_HANDLER = "lastExceptionHandler";
    /**
     * Transport handler (encoder).
     */
    public static final String TRANSPORT_ENCODER = "transportEncoder";
    /**
     * Transport handler (decoder).
     */
    public static final String TRANSPORT_DECODER = "transportDecoder";
    /**
     * Byte handler (encoder).
     */
    public static final String BYTE_ENCODER = "byteEncoder";
    /**
     * Byte handler (decoder).
     */
    public static final String BYTE_DECODER = "byteDecoder";
    /**
     * Channel manager (TCP).
     */
    public static final String CHANNEL_MANAGER = "channelManager";
    /**
     * Message handler.
     */
    public static final String MESSAGE_HANDLER = "messageHandler";
    /**
     * Byte IN/OUT handler.
     */
    public static final String BYTE_LOGGER = "byteLogger";
    /**
     * Message IN/OUT handler.
     */
    public static final String MESSAGE_LOGGER = "messageLogger";


    /**
     * Try to retrieve SPT from {@code Channel}.
     *
     * @param channel the channel on which to retrieve SPT
     *
     * @return the optionally present {@code SptDc09}
     */
    public static Optional<Dc09Spt> fromChannel(final Channel channel) {
        return Optional.ofNullable(channel.attr(SPT).get());
    }

    /**
     * Either add or remove the byte interceptor in the given {@code Channel} pipeline.
     * <p/>
     * Added after the transport decoder.
     *
     * @param flag        whether to add it or to remove it
     * @param channel     the channel on which to add or remove the byte interceptor
     * @param interceptor the byte interceptor to add or remove
     */
    public static void setLogBytesOnChannel(final boolean flag, final Channel channel, final ByteInterceptor interceptor) {
        handleAddRemoveOnChannel(flag, channel, interceptor, BYTE_LOGGER, TRANSPORT_DECODER);
    }

    /**
     * Either add or remove the message interceptor in the given {@code Channel} pipeline.
     * <p/>
     * Added after the byte decoder.
     *
     * @param flag        whether to add it or to remove it
     * @param channel     the channel on which to add or remove the message interceptor
     * @param interceptor the message interceptor to add or remove
     */
    public static void setLogMessagesOnChannel(final boolean flag, final Channel channel, final MessageInterceptor interceptor) {
        handleAddRemoveOnChannel(flag, channel, interceptor, MESSAGE_LOGGER, BYTE_DECODER);
    }

    // Handles add/remove of handler on channel pipeline.
    private static void handleAddRemoveOnChannel(final boolean flag,
                                                 final Channel channel,
                                                 final ChannelHandler handler,
                                                 final String handlerName,
                                                 final String afterHandler) {
        ChannelPipeline pipeline = channel.pipeline();
        ChannelHandler interceptor = pipeline.get(handlerName);
        // When activating and the interceptor is not in pipeline.
        if (flag && interceptor == null) {
            pipeline.addAfter(afterHandler, handlerName, handler);
        } else if (!flag && interceptor != null) { // When deactivating and the interceptor is in the pipeline.
            pipeline.remove(handlerName);
        }
    }

    public static RemoteAddressResolver remoteAddressfromChannel(final Channel channel) {
        if (channel instanceof NioSocketChannel) {
            InetSocketAddress inetSocketAddress = ((NioSocketChannel) channel).remoteAddress();
            return new RemoteAddressResolver.InetSocketAddressResolver(inetSocketAddress);
        }
        return null;
    }

}
