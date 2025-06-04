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

import ch.swissdotnet.siadc09.Dc09Writer;
import ch.swissdotnet.siadc09.holders.ByteOutboundHolder;
import ch.swissdotnet.siadc09.holders.MessageOutboundHolder;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * The {@code MessageEncoder} class encodes SIA DC-09 messages using global parameters and forward it.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
@ChannelHandler.Sharable
public final class MessageEncoder extends MessageToMessageEncoder<MessageOutboundHolder> {

    // DC-09 writer.
    private final Dc09Writer handler;
    // DC-09 global parameters.
    private final Dc09GlobalParameters globalParameters;

    /**
     * Instantiates a new {@code MessageEncoder} with given DC-09 writer and parameters.
     *
     * @param writer           the writer used to encode DC-09 messages
     * @param globalParameters the parameters used to encode DC-09 messages
     */
    public MessageEncoder(final Dc09Writer writer, final Dc09GlobalParameters globalParameters) {
        this.handler = writer;
        this.globalParameters = globalParameters;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx,
                          final MessageOutboundHolder msg,
                          final List<Object> out) throws Exception {

        Dc09Writer.SerializedMessage serialized = handler.write(msg.getMessage(), globalParameters, msg.getSpt().getParameters());
        out.add(new ByteOutboundHolder(serialized.message(), msg.getAddress(), msg.getSpt()));

    }

}
