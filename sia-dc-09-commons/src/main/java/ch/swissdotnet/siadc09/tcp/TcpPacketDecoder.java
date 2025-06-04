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

import ch.swissdotnet.siadc09.Dc09Handler;
import ch.swissdotnet.siadc09.holders.ByteInboundHolder;
import ch.swissdotnet.siadc09.messages.versions.Dc09Version;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

import static ch.swissdotnet.siadc09.ChannelUtilities.remoteAddressfromChannel;
import static ch.swissdotnet.siadc09.Dc09Utils.*;

/**
 * The {@code TcpPacketDecoder} decodes TCP streams, slices bytes from stream and forward it.
 * <p/>
 * Due to some SIA DC-09 implementation using CRC as non-hexadecimal but as binary, it is impossible
 * to use a proper separator as LF or CR could be present in CRC.
 * <p/>
 * To slices TCP streams, first reads LF char, then reads the 4 next bytes (which may be binary CRC + 2 bytes or
 * full hexadecimal CRC) and reads up until CR char.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public final class TcpPacketDecoder extends ReplayingDecoder<TcpPacketDecoder.TcpDecodingStage> {

    // Empty buffer.
    private final byte[] EMPTY_BUFFER = new byte[0];
    // The maximum SIA DC-09 message length (0x3FF).
    private final int maxMessageLength;
    // The buffer to detect CRC.
    private final byte[] crcDetection = new byte[MAXIMUM_PEEK_QUOTE];
    // The current message output;
    private ByteArrayDataOutput output;
    // The current message buffer.
    private byte[] messageBuffer;
    // The current message size.
    private int messageSize = 0;
    // The DC-09 message version.
    private Dc09Version version;

    /**
     * Initializes a new {@code TcpPacketDecoder} with given CRC size and maximum message length.
     *
     * @param maxMessageLength the maximum message length
     */
    public TcpPacketDecoder(final int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
        reset();
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {

        switch (state()) {
            case INIT: {
                byte newline = in.readByte();
                if (newline != LF) {
                    ctx.channel().close();
                    reset();
                    return;
                }
                output.writeByte(newline);
                checkpoint(TcpDecodingStage.LF);
            }
            case LF: {
                in.readBytes(crcDetection, 0, MAXIMUM_PEEK_QUOTE);
                version = Dc09Handler.buildVersion(Dc09Handler.detectDc09Version(crcDetection));
                output.write(crcDetection);
                checkpoint(TcpDecodingStage.CRC_LENGTH);
            }
            case CRC_LENGTH: {
                int crcSize = version.crcSize();
                int messageLength = version.lengthSize();
                byte[] lengthBuffer = new byte[messageLength];
                System.arraycopy(crcDetection, crcSize + 1, lengthBuffer, 0, messageLength);
                messageSize = hexBytesToInt(lengthBuffer);
                if (messageSize > maxMessageLength) {
                    ctx.channel().close();
                    reset();
                    return;
                }
                int alreadyReadBytes = MAXIMUM_PEEK_QUOTE - crcSize - messageLength - 1;
                messageBuffer = new byte[messageSize - alreadyReadBytes];
                checkpoint(TcpDecodingStage.MESSAGE);
            }
            case MESSAGE: {
                in.readBytes(messageBuffer);
                output.write(messageBuffer);
                checkpoint(TcpDecodingStage.CR);
            }
            case CR: {
                byte carriageReturn = in.readByte();
                if (carriageReturn != CR) {
                    ctx.channel().close();
                    return;
                }
                output.write(carriageReturn);
                // Retrieve bytes.
                byte[] message = output.toByteArray();
                try {
                    out.add(new ByteInboundHolder(message, remoteAddressfromChannel(ctx.channel())));
                } finally {
                    reset();
                }
            }
        }

    }

    private void reset() {
        messageSize = 0;
        output = ByteStreams.newDataOutput();
        messageBuffer = EMPTY_BUFFER;
        checkpoint(TcpDecodingStage.INIT);
    }

    public enum TcpDecodingStage {
        INIT,
        LF,
        CRC_LENGTH,
        MESSAGE,
        CR
    }

}
