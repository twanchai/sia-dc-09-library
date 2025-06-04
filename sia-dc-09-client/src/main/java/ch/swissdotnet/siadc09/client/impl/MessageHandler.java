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
package ch.swissdotnet.siadc09.client.impl;

import ch.swissdotnet.siadc09.RemoteAddressResolver;
import ch.swissdotnet.siadc09.client.ClientResponseListener;
import ch.swissdotnet.siadc09.holders.MessageInboundHolder;
import ch.swissdotnet.siadc09.holders.MessageOutboundHolder;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.ScheduledFuture;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class MessageHandler extends ChannelDuplexHandler {

    private final int timeout;
    private final ClientResponseListener listener;
    private final Queue<MessageOutboundHolder> messages;
    private final AtomicBoolean waiting = new AtomicBoolean(false);
    private final AtomicReference<Message> currentMessage = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> currentTimeout = new AtomicReference<>();

    public MessageHandler(final Queue<MessageOutboundHolder> messages,
                          final int timeout,
                          final ClientResponseListener listener) {
        this.timeout = timeout;
        this.messages = messages;
        this.listener = listener;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (msg instanceof final MessageInboundHolder inbound) {
            ScheduledFuture<?> timeout = currentTimeout.get();
            if (timeout != null) {
                timeout.cancel(true);
                currentTimeout.set(null);
            }
            Message message = inbound.getMessage();
            Message sentMessage = currentMessage.get();
            listener.onResponse(message, sentMessage);
            waiting.set(false);
            writeNextOnChannel(ctx);
        }
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
        if (msg instanceof final MessageHolder holder) {
            Channel channel = ctx.channel();
            MessageOutboundHolder outboundHolder = new MessageOutboundHolder(
                holder.getMessage(),
                new RemoteAddressResolver.InetSocketAddressResolver((InetSocketAddress) channel.remoteAddress()),
                holder.getSpt()
            );
            writeOnChannel(outboundHolder, ctx);
        }
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

    private void writeOnChannel(final MessageOutboundHolder outboundHolder, final ChannelHandlerContext ctx) {
        if (waiting.compareAndSet(false, true)) {
            currentMessage.set(outboundHolder.getMessage());
            ScheduledFuture<?> future = ctx.channel().eventLoop().schedule(
                new MessageTimeout(listener, outboundHolder.getMessage(), ctx), timeout, TimeUnit.SECONDS
            );
            currentTimeout.set(future);
            ctx.writeAndFlush(outboundHolder);
        } else {
            messages.add(outboundHolder);
        }
    }

    private void writeNextOnChannel(final ChannelHandlerContext ctx) {
        if (!messages.isEmpty()) {
            writeOnChannel(messages.poll(), ctx);
        }
    }

    public static final class MessageHolder {

        private final Message message;
        private final Dc09Spt spt;

        public MessageHolder(final Dc09Spt spt, final Message message) {
            this.message = message;
            this.spt = spt;
        }

        public Message getMessage() {
            return message;
        }

        public Dc09Spt getSpt() {
            return spt;
        }
    }

    private class MessageTimeout implements Runnable {

        private final ClientResponseListener listener;
        private final Message message;
        private final ChannelHandlerContext ctx;

        public MessageTimeout(final ClientResponseListener listener,
                              final Message message,
                              final ChannelHandlerContext ctx) {
            this.listener = listener;
            this.message = message;
            this.ctx = ctx;
        }

        @Override
        public void run() {
            listener.onMessageTimeout(message);
            waiting.set(false);
            currentTimeout.set(null);
            writeNextOnChannel(ctx);
        }
    }
}
