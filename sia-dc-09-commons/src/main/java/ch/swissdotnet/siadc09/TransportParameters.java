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
package ch.swissdotnet.siadc09;

import java.util.concurrent.TimeUnit;

/**
 * The {@code ServerParameters} class serves as configuration for both TCP and UDP DC-09 servers.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public final class TransportParameters {

    // Whether to log bytes or not.
    private boolean logBytes = false;
    // Whether to log messages or not.
    private boolean logMessages = false;
    // How many threads to create for channel.
    private int channelThread = 10;
    // Maximum threads for onMessage handling.
    private int onMessageThreads = 512;
    // Maximum keep alive on message threads.
    private int onMessageThreadsKeepAlive = 10;
    // Maximum keep alive on message threads time unit.
    private TimeUnit onMessageThreadsKeepAliveTimeUnit = TimeUnit.SECONDS;
    // The amount of time (ns) to wait when a channel has been interrupted.
    private long awaitAfterChannelInterruptedNs = TimeUnit.SECONDS.toNanos(5);

    /**
     * @return whether to log bytes or not
     */
    public boolean isLogBytes() {
        return logBytes;
    }

    /**
     * Sets whether to log bytes or not
     *
     * @param logBytes whether the log bytes or not
     *
     * @return the {@code ServerParameters} instance
     */
    public TransportParameters setLogBytes(final boolean logBytes) {
        this.logBytes = logBytes;
        return this;
    }

    /**
     * @return whether to log messages or not
     */
    public boolean isLogMessages() {
        return logMessages;
    }

    /**
     * Sets whether to log messages or not
     *
     * @param logMessages whether the log messages or not
     *
     * @return the {@code ServerParameters} instance
     */
    public TransportParameters setLogMessages(final boolean logMessages) {
        this.logMessages = logMessages;
        return this;
    }

    /**
     * @return how many threads to create to handle channel reads/writes
     */
    public int getChannelThread() {
        return channelThread;
    }

    /**
     * Sets how many threads to create to handle channel reads/writes
     *
     * @param channelThread the thread count
     *
     * @return the {@code ServerParameters} instance
     */
    public TransportParameters setChannelThread(final int channelThread) {
        this.channelThread = channelThread;
        return this;
    }

    public int getOnMessageThreads() {
        return onMessageThreads;
    }

    public TransportParameters setOnMessageThreads(final int onMessageThreads) {
        this.onMessageThreads = onMessageThreads;
        return this;
    }

    public int getOnMessageThreadsKeepAlive() {
        return onMessageThreadsKeepAlive;
    }

    public TransportParameters setOnMessageThreadsKeepAlive(final int onMessageThreadsKeepAlive) {
        this.onMessageThreadsKeepAlive = onMessageThreadsKeepAlive;
        return this;
    }

    public TimeUnit getOnMessageThreadsKeepAliveTimeUnit() {
        return onMessageThreadsKeepAliveTimeUnit;
    }

    public TransportParameters setOnMessageThreadsKeepAliveTimeUnit(final TimeUnit onMessageThreadsKeepAliveTimeUnit) {
        this.onMessageThreadsKeepAliveTimeUnit = onMessageThreadsKeepAliveTimeUnit;
        return this;
    }

    public long getAwaitAfterChannelInterruptedNs() {
        return awaitAfterChannelInterruptedNs;
    }

    public TransportParameters setAwaitAfterChannelInterruptedNs(final long awaitAfterChannelInterruptedNs) {
        this.awaitAfterChannelInterruptedNs = awaitAfterChannelInterruptedNs;
        return this;
    }

}
