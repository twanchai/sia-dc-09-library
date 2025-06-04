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

import ch.swissdotnet.siadc09.Dc09SptStore;
import ch.swissdotnet.siadc09.MessageListener;
import ch.swissdotnet.siadc09.holders.MessageInboundHolder;
import ch.swissdotnet.siadc09.holders.MessageOutboundHolder;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.Optional;
import java.util.Set;

import static ch.swissdotnet.siadc09.MessageListener.Direction.INCOMING;
import static ch.swissdotnet.siadc09.MessageListener.Direction.OUTGOING;

/**
 * The {@code ByteInterceptor} class intercepts both incoming and outgoing SIA DC-09 messages.
 * <p/>
 * Upon receptions, it calls all {@code Dc09MessageListener}s with given read/write message.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
@ChannelHandler.Sharable
public final class MessageInterceptor extends ChannelDuplexHandler {

    // DC-09 Store.
    private final Dc09SptStore store;
    // All listeners to notify on message read/write.
    private final Set<? extends MessageListener> listeners;

    /**
     * Instantiates a new {@code MessageInterceptor} with given listeners.
     *
     * @param store the SPT store to retrieve devices from
     * @param listeners the listeners to call upon message read/write
     */
    public MessageInterceptor(final Dc09SptStore store, final Set<? extends MessageListener> listeners) {
        this.store = store;
        this.listeners = listeners;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof final MessageInboundHolder holder) {
            Optional<Dc09Spt> optSpt = store.retrieve(holder.getMessage());
            for (MessageListener listener : listeners) {
                listener.onMessageLog(holder.getMessage(), holder.getAddress(), optSpt, INCOMING);
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof final MessageOutboundHolder holder) {
            Optional<Dc09Spt> optSpt = store.retrieve(holder.getMessage());
            for (MessageListener listener : listeners) {
                listener.onMessageLog(holder.getMessage(), holder.getAddress(), optSpt, OUTGOING);
            }
        }
        ctx.write(msg);
    }

}
