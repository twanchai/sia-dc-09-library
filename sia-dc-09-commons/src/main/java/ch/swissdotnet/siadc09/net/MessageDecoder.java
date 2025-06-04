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
package ch.swissdotnet.siadc09.net;

import ch.swissdotnet.siadc09.Dc09Reader;
import ch.swissdotnet.siadc09.Dc09SptStore;
import ch.swissdotnet.siadc09.Dc09Utils;
import ch.swissdotnet.siadc09.exceptions.Dc09Exception;
import ch.swissdotnet.siadc09.exceptions.InvalidMessageException;
import ch.swissdotnet.siadc09.holders.ByteInboundHolder;
import ch.swissdotnet.siadc09.holders.MessageInboundHolder;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import static ch.swissdotnet.siadc09.ChannelUtilities.REMOTE_ADDRESS_RESOLVER;

/**
 * The {@code MessageDecoder} class receives byte content, parses it using global parameters and forward it.
 * <p/>
 * Whenever a message is invalid, it throws a {@code Dc09Exception}.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
@ChannelHandler.Sharable
public final class MessageDecoder extends MessageToMessageDecoder<ByteInboundHolder> {

    // The DC-09 message reader.
    private final Dc09Reader reader;
    // The DC-09 global parameters.
    private final Dc09GlobalParameters globalParameters;
    // The DC-09 SPT store used to retrieve SPT by its account number.
    private final Dc09SptStore store;

    /**
     * Instantiates a new {@code MessageDecoder} with given DC-09 reader, parameters and store.
     *
     * @param reader           the DC-09 message reader
     * @param globalParameters the DC-09 global parameters
     * @param store            the DC-09 SPT store
     */
    public MessageDecoder(final Dc09Reader reader, final Dc09GlobalParameters globalParameters, final Dc09SptStore store) {
        this.reader = reader;
        this.globalParameters = globalParameters;
        this.store = store;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteInboundHolder msg, final List<Object> out) throws Exception {
        byte[] content = msg.getContent();
        ctx.channel().attr(REMOTE_ADDRESS_RESOLVER).set(msg.getAddress());
        try {
            Message message = reader.read(content, globalParameters, store);
            out.add(new MessageInboundHolder(message, msg.getAddress()));
        } catch (final InvalidMessageException e) {
            String message = "Invalid message content: " + Dc09Utils.bytesToHexString(content);
            throw new Dc09Exception(message, e).setAccountNumber(e.getAccountNumber().orElse(null));
        }
    }

}
