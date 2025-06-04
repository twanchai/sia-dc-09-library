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

import ch.swissdotnet.siadc09.RemoteAddressResolver;
import ch.swissdotnet.siadc09.holders.ByteInboundHolder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

/**
 * The {@code UdpDatagramDecoder} class handles {@code DatagramPacket} received on the wire.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
@ChannelHandler.Sharable
public final class UdpDatagramDecoder extends SimpleChannelInboundHandler<DatagramPacket> {

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket msg) {
        ByteBuf msgBuffer = msg.content();
        byte[] content = new byte[msgBuffer.readableBytes()];
        msgBuffer.readBytes(content);
        ctx.fireChannelRead(new ByteInboundHolder(content, new RemoteAddressResolver.InetSocketAddressResolver(msg.sender())));
    }

}
