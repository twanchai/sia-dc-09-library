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
package ch.swissdotnet.siadc09.udp;

import ch.swissdotnet.siadc09.holders.ByteInboundHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * The {@code UdpDatagramEncoder} class handles {@code ByteHolder} to send on the wire through {@code DatagramPacket}.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
@ChannelHandler.Sharable
public final class UdpDatagramEncoder extends MessageToMessageEncoder<ByteInboundHolder> {

    @Override
    protected void encode(final ChannelHandlerContext ctx, final ByteInboundHolder msg, final List<Object> out) {
        out.add(new DatagramPacket(Unpooled.wrappedBuffer(msg.getContent()), msg.getAddress().getInetSocketAddress()));
    }

}
