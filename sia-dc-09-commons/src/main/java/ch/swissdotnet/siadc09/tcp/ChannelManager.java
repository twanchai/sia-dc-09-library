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
package ch.swissdotnet.siadc09.tcp;

import ch.swissdotnet.siadc09.log.ByteInterceptor;
import ch.swissdotnet.siadc09.log.MessageInterceptor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import static ch.swissdotnet.siadc09.ChannelUtilities.setLogBytesOnChannel;
import static ch.swissdotnet.siadc09.ChannelUtilities.setLogMessagesOnChannel;

/**
 * The {@code ChannelManager} allows to manage TCP channels to add/remove handlers to {@code Channel} pipeline.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
@ChannelHandler.Sharable
public final class ChannelManager extends ChannelDuplexHandler {

    // Channel group to iterate upon channels.
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final ByteInterceptor byteInterceptor;
    private final MessageInterceptor messageInterceptor;

    /**
     * Instantiates a new {@code ChannelManager} which manages all TCP channels.
     *
     * @param byteInterceptor    the byte interceptor used to log bytes
     * @param messageInterceptor the message interceptor used to log messages
     */
    public ChannelManager(final ByteInterceptor byteInterceptor, final MessageInterceptor messageInterceptor) {
        this.byteInterceptor = byteInterceptor;
        this.messageInterceptor = messageInterceptor;
    }

    /**
     * @return the byte interceptor
     */
    public ByteInterceptor getByteInterceptor() {
        return byteInterceptor;
    }

    /**
     * @return the message interceptor
     */
    public MessageInterceptor getMessageInterceptor() {
        return messageInterceptor;
    }

    /**
     * Either add or remove byte interceptor to log incoming and outgoing bytes.
     * <p/>
     * Only add the interceptor once, multiple calls with true value will not duplicate it.
     *
     * @param flag whether to add or remove byte interceptor
     */
    public void logBytes(final boolean flag) {
        for (Channel channel : channels) {
            setLogBytesOnChannel(flag, channel, byteInterceptor);
        }
    }

    /**
     * Either add or remove message interceptor to log incoming and outgoing messages.
     * <p/>
     * Only add the interceptor once, multiple calls with true value will not duplicate it.
     *
     * @param flag whether to add or remove message interceptor
     */
    public void logMessages(final boolean flag) {
        for (Channel channel : channels) {
            setLogMessagesOnChannel(flag, channel, messageInterceptor);
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        channels.add(ctx.channel());
    }
}
