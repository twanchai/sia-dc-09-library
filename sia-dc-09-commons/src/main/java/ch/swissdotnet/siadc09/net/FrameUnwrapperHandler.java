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

import ch.swissdotnet.siadc09.holders.ByteInboundHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code FrameUnwrapperHandler} Netty handler invokes a {@link FrameUnwrapper} on every
 * incoming {@link ByteInboundHolder} before it reaches the SIA DC-09 parser.
 *
 * <p>If {@link FrameUnwrapper#tryUnwrap} returns non-{@code null} the content of the holder
 * is replaced with the unwrapped bytes. Otherwise the original bytes are forwarded unchanged
 * (transparent fallback).</p>
 */
@ChannelHandler.Sharable
public final class FrameUnwrapperHandler extends SimpleChannelInboundHandler<ByteInboundHolder> {

    private static final Logger LOG = LoggerFactory.getLogger(FrameUnwrapperHandler.class);

    private final FrameUnwrapper unwrapper;

    /**
     * Instantiates a new {@code FrameUnwrapperHandler} with given unwrapper.
     *
     * @param unwrapper the frame unwrapper to apply
     */
    public FrameUnwrapperHandler(final FrameUnwrapper unwrapper) {
        this.unwrapper = unwrapper;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ByteInboundHolder msg) {
        byte[] original = msg.getContent();
        byte[] unwrapped = null;
        try {
            unwrapped = unwrapper.tryUnwrap(original);
        } catch (Exception e) {
            LOG.warn("FrameUnwrapper threw an exception — forwarding original bytes. Cause: {}", e.getMessage(), e);
        }
        ctx.fireChannelRead(new ByteInboundHolder(
                unwrapped != null ? unwrapped : original,
                msg.getAddress()));
    }

}
