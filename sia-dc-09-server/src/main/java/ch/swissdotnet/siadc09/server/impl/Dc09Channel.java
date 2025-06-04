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

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.SocketAddress;
import java.util.Objects;


public interface Dc09Channel extends Channel {

    class TcpChannel extends AbstractChannel {

        TcpChannel(final Channel forward) {
            super(forward);
        }
    }

    class UdpChannel extends AbstractChannel {

        UdpChannel(final Channel forward) {
            super(forward);
        }

        @Override
        public ChannelFuture close() {
            return super.forward.disconnect();
        }
    }

    abstract class AbstractChannel implements Dc09Channel {

        private final Channel forward;

        AbstractChannel(final Channel forward) {
            this.forward = Objects.requireNonNull(forward, "Channel must be non-null.");
        }

        @Override
        public ChannelId id() {
            return forward.id();
        }

        @Override
        public EventLoop eventLoop() {
            return forward.eventLoop();
        }

        @Override
        public Channel parent() {
            return forward.parent();
        }

        @Override
        public ChannelConfig config() {
            return forward.config();
        }

        @Override
        public boolean isOpen() {
            return forward.isOpen();
        }

        @Override
        public boolean isRegistered() {
            return forward.isRegistered();
        }

        @Override
        public boolean isActive() {
            return forward.isActive();
        }

        @Override
        public ChannelMetadata metadata() {
            return forward.metadata();
        }

        @Override
        public SocketAddress localAddress() {
            return forward.localAddress();
        }

        @Override
        public SocketAddress remoteAddress() {
            return forward.remoteAddress();
        }

        @Override
        public ChannelFuture closeFuture() {
            return forward.closeFuture();
        }

        @Override
        public boolean isWritable() {
            return forward.isWritable();
        }

        @Override
        public long bytesBeforeUnwritable() {
            return forward.bytesBeforeUnwritable();
        }

        @Override
        public long bytesBeforeWritable() {
            return forward.bytesBeforeWritable();
        }

        @Override
        public Unsafe unsafe() {
            return forward.unsafe();
        }

        @Override
        public ChannelPipeline pipeline() {
            return forward.pipeline();
        }

        @Override
        public ByteBufAllocator alloc() {
            return forward.alloc();
        }

        @Override
        public Channel read() {
            return forward.read();
        }

        @Override
        public Channel flush() {
            return forward.flush();
        }

        @Override
        public ChannelFuture bind(final SocketAddress localAddress) {
            return forward.bind(localAddress);
        }

        @Override
        public ChannelFuture connect(final SocketAddress remoteAddress) {
            return forward.connect(remoteAddress);
        }

        @Override
        public ChannelFuture connect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
            return forward.connect(remoteAddress, localAddress);
        }

        @Override
        public ChannelFuture disconnect() {
            return forward.disconnect();
        }

        @Override
        public ChannelFuture close() {
            return forward.close();
        }

        @Override
        public ChannelFuture deregister() {
            return forward.deregister();
        }

        @Override
        public ChannelFuture bind(final SocketAddress localAddress, final ChannelPromise promise) {
            return forward.bind(localAddress, promise);
        }

        @Override
        public ChannelFuture connect(final SocketAddress remoteAddress, final ChannelPromise promise) {
            return forward.connect(remoteAddress, promise);
        }

        @Override
        public ChannelFuture connect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
            return forward.connect(remoteAddress, localAddress, promise);
        }

        @Override
        public ChannelFuture disconnect(final ChannelPromise promise) {
            return forward.disconnect(promise);
        }

        @Override
        public ChannelFuture close(final ChannelPromise promise) {
            return forward.close(promise);
        }

        @Override
        public ChannelFuture deregister(final ChannelPromise promise) {
            return forward.deregister(promise);
        }

        @Override
        public ChannelFuture write(final Object msg) {
            return forward.write(msg);
        }

        @Override
        public ChannelFuture write(final Object msg, final ChannelPromise promise) {
            return forward.write(msg, promise);
        }

        @Override
        public ChannelFuture writeAndFlush(final Object msg, final ChannelPromise promise) {
            return forward.writeAndFlush(msg, promise);
        }

        @Override
        public ChannelFuture writeAndFlush(final Object msg) {
            return forward.writeAndFlush(msg);
        }

        @Override
        public ChannelPromise newPromise() {
            return forward.newPromise();
        }

        @Override
        public ChannelProgressivePromise newProgressivePromise() {
            return forward.newProgressivePromise();
        }

        @Override
        public ChannelFuture newSucceededFuture() {
            return forward.newSucceededFuture();
        }

        @Override
        public ChannelFuture newFailedFuture(final Throwable cause) {
            return forward.newFailedFuture(cause);
        }

        @Override
        public ChannelPromise voidPromise() {
            return forward.voidPromise();
        }

        @Override
        public <T> Attribute<T> attr(final AttributeKey<T> key) {
            return forward.attr(key);
        }

        @Override
        public <T> boolean hasAttr(final AttributeKey<T> key) {
            return forward.hasAttr(key);
        }

        @Override
        public int compareTo(final Channel o) {
            return forward.compareTo(o);
        }
    }

}
