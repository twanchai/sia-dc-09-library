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
import ch.swissdotnet.siadc09.RemoteAddressResolver;
import ch.swissdotnet.siadc09.exceptions.Dc09Exception;
import ch.swissdotnet.siadc09.holders.MessageInboundHolder;
import ch.swissdotnet.siadc09.holders.MessageOutboundHolder;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.server.RctDc09;
import ch.swissdotnet.siadc09.server.listeners.ServerMessageListener;
import ch.swissdotnet.siadc09.server.listeners.ServerResponseListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static ch.swissdotnet.siadc09.ChannelUtilities.SPT;
import static ch.swissdotnet.siadc09.ChannelUtilities.fromChannel;

/**
 * The {@code MessageHandler} class handles all inbound SIA DC-09 messages by forwarding it to DC-09 listeners.
 * <p/>
 * Upon reception, search through {@code SptStore} which SPT sent the message. If the SPT is registered, the message
 * is forwarded, otherwise the channel is closed and a {@code Dc09Exception} is thrown to indicate an error with
 * given account number.
 * <p/>
 * When unregistered, a {@code Dc09Exception} is thrown to indicate which SPT concerned by the exchanged disconnected
 * (TCP specific).
 * <p/>
 * Of all {@code Dc09MessageListener}s, only the first to responds is used.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
@ChannelHandler.Sharable
public final class MessageHandler extends SimpleChannelInboundHandler<MessageInboundHolder> {

    // SLF4J Logger
    private static final Logger LOG = LoggerFactory.getLogger(MessageHandler.class);
    // DC-09 SPT store.
    private final Dc09SptStore store;
    // DC-09 RCT (itself).
    private final RctDc09 rct;
    // The executor on which onMessage are sent.
    private final ExecutorService onMessageExecutor;
    // All listeners to call upon message reception.
    private final Set<ServerMessageListener> listeners;
    // Factory to transform channels to DC-09 channels which allow more control over transport specifics.
    private final Function<Channel, Dc09Channel> dc09ChannelFactory;

    /**
     * Instantiates a new {@code MessageHandler} with given store, RCT and listeners.
     *
     * @param store             the DC-09 store used to retrieve SPT from account number
     * @param rct               the DC-09 RCT on which the message is received
     * @param onMessageExecutor the executor on which on message are sent
     * @param listeners         the listeners to call upon message reception
     */
    public MessageHandler(final Dc09SptStore store,
                          final RctDc09 rct,
                          final ExecutorService onMessageExecutor,
                          final Set<ServerMessageListener> listeners,
                          final Function<Channel, Dc09Channel> dc09ChannelFactory) {
        this.store = store;
        this.rct = rct;
        this.onMessageExecutor = onMessageExecutor;
        this.listeners = listeners;
        this.dc09ChannelFactory = dc09ChannelFactory;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final MessageInboundHolder inbound) throws Exception {

        Message message = inbound.getMessage();
        // Tries to find the SPT concerned by the message.
        Optional<Dc09Spt> optParameters = store.retrieve(message);
        Dc09Channel channel = dc09ChannelFactory.apply(ctx.channel());

        // When the SPT is found, calls all listeners with their own response handler.
        if (optParameters.isPresent()) {
            Dc09Spt dc09Spt = optParameters.get();
            channel.attr(SPT).set(dc09Spt);
            DateTime received = LocalDateTime.now(DateTimeZone.UTC).toDateTime(DateTimeZone.UTC);
            AtomicBoolean used = new AtomicBoolean(false);
            for (ServerMessageListener listener : listeners) {
                onMessageExecutor.execute(
                    () -> listener.onMessage(
                        message,
                        received,
                        dc09Spt,
                        rct,
                        inbound.getAddress(),
                        new ServerResponseHandler(dc09Spt, channel, inbound.getAddress(), used)
                    )
                );
            }
        }
        // The SPT has not been found.
        else {
            channel.close();
            String accountNumber = message.getAccountNumber();
            throw new Dc09Exception("Unable to find transmitter with account number " + accountNumber + ".")
                .setAccountNumber(accountNumber);
        }

    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        Optional<Dc09Spt> optSpt = fromChannel(ctx.channel());
        if (optSpt.isPresent()) {
            Dc09Spt spt = optSpt.get();
            throw new Dc09Exception("Connexion closed with account number " + spt.getAccountNumber() + ".")
                .setSptDc09(spt);
        }
    }

    // Listener implementation which prevents from giving multiple response to SPT.
    private static class ServerResponseHandler implements ServerResponseListener {

        // SPT concerned by the message.
        private final Dc09Spt dc09Spt;
        // Channel involved in the transmission.
        private final Dc09Channel channel;
        // Sender address.
        private final RemoteAddressResolver sender;
        // Whether response has already been given or not.
        private final AtomicBoolean used;

        // Instantiates a new handler used to responds to SPT.
        private ServerResponseHandler(final Dc09Spt dc09Spt,
                                      final Dc09Channel channel,
                                      final RemoteAddressResolver sender,
                                      final AtomicBoolean used) {
            this.dc09Spt = dc09Spt;
            this.channel = channel;
            this.sender = sender;
            this.used = used;
        }

        @Override
        public void response(final Message message) {
            if (used.compareAndSet(false, true)) {
                channel.writeAndFlush(new MessageOutboundHolder(message, sender, dc09Spt));
            } else {
                LOG.warn("Tried to give multiple response to DC-09 SPT.");
            }
        }

        @Override
        public void close() {
            if (used.compareAndSet(false, true)) {
                channel.close();
            } else {
                LOG.warn("Tried to give multiple response to DC-09 SPT.");
            }
        }
    }

}
