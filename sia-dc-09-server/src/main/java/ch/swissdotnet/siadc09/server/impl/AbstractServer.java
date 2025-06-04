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

import ch.swissdotnet.siadc09.TransportParameters;
import ch.swissdotnet.siadc09.server.RctDc09;
import ch.swissdotnet.siadc09.server.SiaDc09Server;
import ch.swissdotnet.siadc09.server.listeners.ServerMessageListener;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.Channel;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The {@code AbstractServer} abstract class represents the base from both TCP and UDP server.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
abstract class AbstractServer implements SiaDc09Server {

    // The RCT it represents.
    final RctDc09 rct;
    // DC-09 messages listeners to call upon event.
    final Set<ServerMessageListener> listeners = Sets.newConcurrentHashSet();
    // The executor on which onMessage (MessageHandler) are sent with.
    final ExecutorService onMessageExecutor;
    // Store whether the server is running or not.
    final AtomicBoolean running = new AtomicBoolean(false);
    // Server parameters (i.e. log bytes/messages, channel threads, ...).
    private final TransportParameters transportParameters;
    // Channel once the server is running.
    volatile List<Channel> channels = Lists.newCopyOnWriteArrayList();

    // Initializes the AbstractSever with given parameters.
    AbstractServer(final RctDc09 rct,
                   final TransportParameters transportParameters,
                   final ExecutorService onMessageExecutor) {
        this.rct = rct;
        this.transportParameters = transportParameters;
        this.onMessageExecutor = onMessageExecutor;
    }

    @Override
    public SiaDc09Server addMessageListener(final ServerMessageListener listener) {
        this.listeners.add(listener);
        return this;
    }

    @Override
    public SiaDc09Server removeMessageListener(final ServerMessageListener listener) {
        this.listeners.remove(listener);
        return this;
    }

    @Override
    public SiaDc09Server setLogBytes(final boolean flag) {
        transportParameters.setLogBytes(flag);
        return this;
    }

    @Override
    public SiaDc09Server setLogMessages(final boolean flag) {
        transportParameters.setLogMessages(flag);
        return this;
    }

    @Override
    public void close() throws Exception {
        running.set(false);
        onMessageExecutor.shutdownNow();
    }
}
