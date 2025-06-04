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
package ch.swissdotnet.siadc09.tests;

import ch.swissdotnet.siadc09.holders.ByteInboundHolder;
import ch.swissdotnet.siadc09.messages.versions.StandardDc09;
import ch.swissdotnet.siadc09.tcp.TcpPacketDecoder;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


public class TcpPacketDecoderTest {

    private final int maximumLength = new StandardDc09().maxMessageLength();

    @Test
    public void testStandardDc09Packet() {
        byte[] content = "\nE34C0062\"*ACK\"0001R5678L1234#080027E62A64[14F088FF5DCB19D1908069507EAB97C7CDB20F1C6EFA550BB59864D54B2DFA1C\r".getBytes();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new TcpPacketDecoder(maximumLength));
        embeddedChannel.writeInbound(Unpooled.wrappedBuffer(content));
        ByteInboundHolder byteInboundHolder = embeddedChannel.readInbound();
        Assertions.assertArrayEquals(content, byteInboundHolder.getContent());
    }

    @Test
    public void testBinaryDc09Packet() {
        byte[] crc = {UnsignedBytes.checkedCast('\u00e3'), UnsignedBytes.checkedCast('\u004c')};
        byte[] content = "\n  0062\"*ACK\"0001R5678L1234#080027E62A64[14F088FF5DCB19D1908069507EAB97C7CDB20F1C6EFA550BB59864D54B2DFA1C\r".getBytes();
        content[1] = crc[0];
        content[2] = crc[1];
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new TcpPacketDecoder(maximumLength));
        embeddedChannel.writeInbound(Unpooled.wrappedBuffer(content));
        ByteInboundHolder byteInboundHolder = embeddedChannel.readInbound();
        Assertions.assertArrayEquals(content, byteInboundHolder.getContent());
    }

    @Test
    public void testStandardDc09PacketInParts() {
        byte[] content = "\nE34C0062\"*ACK\"0001R5678L1234#080027E62A64[14F088FF5DCB19D1908069507EAB97C7CDB20F1C6EFA550BB59864D54B2DFA1C\r".getBytes();
        List<byte[]> parts = Lists.newArrayList(
            "\nE34C".getBytes(),
            "0062\"*AC".getBytes(),
            "K\"0001R5678L1234#08002".getBytes(),
            "7E62A64[14F088FF5DCB19D1".getBytes(),
            "908069507EAB97C7CDB20F1C6EFA550BB59864D54B2DFA1C".getBytes(),
            "\r".getBytes()
        );
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new TcpPacketDecoder(maximumLength));
        for (int i = 0; i < parts.size(); i++) {
            final byte[] part = parts.get(i);
            embeddedChannel.writeInbound(Unpooled.wrappedBuffer(part));
            if (i < parts.size() - 1) {
                ByteInboundHolder byteInboundHolder = embeddedChannel.readInbound();
                Assertions.assertNull(byteInboundHolder);
            }
        }
        ByteInboundHolder byteInboundHolder = embeddedChannel.readInbound();
        Assertions.assertNotNull(byteInboundHolder);
        Assertions.assertArrayEquals(content, byteInboundHolder.getContent());
    }

}
