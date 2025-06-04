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
package ch.swissdotnet.siadc09.log;

import ch.swissdotnet.siadc09.MessageListener;
import ch.swissdotnet.siadc09.holders.ByteInboundHolder;
import ch.swissdotnet.siadc09.holders.ByteOutboundHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.Optional;
import java.util.Set;

import static ch.swissdotnet.siadc09.MessageListener.Direction.OUTGOING;

/**
 * The {@code ByteInterceptor} class intercepts both incoming and outgoing byte array.
 * <p/>
 * Upon receptions, it calls all {@code Dc09MessageListener}s with given read/write byte.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
@ChannelHandler.Sharable
public final class ByteInterceptor extends ChannelDuplexHandler {

    // All listeners to notify on byte read/write.
    private final Set<? extends MessageListener> listeners;

    /**
     * Instantiates a new {@code ByteInterceptor} with given listeners.
     *
     * @param listeners the listeners to call upon byte read/write
     */
    public ByteInterceptor(final Set<? extends MessageListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof ByteInboundHolder) {
            ByteOutboundHolder holder = (ByteOutboundHolder) msg;
            for (MessageListener listener : listeners) {
                listener.onByteLog(
                    holder.getContent(), holder.getAddress(), Optional.ofNullable(holder.getSpt()),
                    OUTGOING
                );
            }
        }
        ctx.write(msg);
    }

}
