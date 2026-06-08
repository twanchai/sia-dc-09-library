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
package ch.swissdotnet.siadc09.server;

import ch.swissdotnet.siadc09.Dc09SptStore;
import ch.swissdotnet.siadc09.TransportParameters;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.net.FrameUnwrapper;
import ch.swissdotnet.siadc09.server.impl.TcpSiaDc09Server;
import ch.swissdotnet.siadc09.server.impl.UdpSiaDc09Server;
import ch.swissdotnet.siadc09.server.listeners.ServerMessageListener;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * The {@code SiaDc09Servers} class is used to manage multiple server instances by creating an
 * instance for each {@code RctDc09} given.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public final class SiaDc09Servers implements SiaDc09Server {

    // SLF4J Logger
    private static final Logger LOG = LoggerFactory.getLogger(SiaDc09Servers.class);
    // All servers created.
    private final List<SiaDc09Server> servers = Lists.newArrayList();
    // The executor on which onMessage (MessageHandler) are sent with.
    private final ExecutorService onMessageExecutor;
    // The service executor used to create servers.
    private final ExecutorService executor;
    // Optional proprietary frame unwrapper.
    private final FrameUnwrapper frameUnwrapper;

    /**
     * Initializes a new {@code SiaDc09Servers} with given {@code RctDc09} and parameters.
     *
     * @param rcts                the servers to create
     * @param transportParameters the server parameters to use
     * @param store               the DC-09 SPT store
     * @param globalParameters    the DC-09 global parameters
     */
    public SiaDc09Servers(final List<RctDc09> rcts,
                          final TransportParameters transportParameters,
                          final Dc09SptStore store,
                          final Dc09GlobalParameters globalParameters,
                          final Optional<ThreadFactory> threadPoolThreadFactory,
                          final Optional<ThreadFactory> serverThreadFactory) {
        this(rcts, transportParameters, store, globalParameters,
             threadPoolThreadFactory, serverThreadFactory, null);
    }

    /**
     * Initializes a new {@code SiaDc09Servers} with given {@code RctDc09}, parameters and frame unwrapper.
     *
     * @param rcts                      the servers to create
     * @param transportParameters       the server parameters to use
     * @param store                     the DC-09 SPT store
     * @param globalParameters          the DC-09 global parameters
     * @param threadPoolThreadFactory   optional thread factory for the onMessage pool
     * @param serverThreadFactory       optional thread factory for server threads
     * @param frameUnwrapper            optional proprietary frame unwrapper (e.g. DSC binary wrapper)
     */
    public SiaDc09Servers(final List<RctDc09> rcts,
                          final TransportParameters transportParameters,
                          final Dc09SptStore store,
                          final Dc09GlobalParameters globalParameters,
                          final Optional<ThreadFactory> threadPoolThreadFactory,
                          final Optional<ThreadFactory> serverThreadFactory,
                          final FrameUnwrapper frameUnwrapper) {

        this.frameUnwrapper = frameUnwrapper;
        onMessageExecutor = new ThreadPoolExecutor(
            0,
            transportParameters.getOnMessageThreads(),
            transportParameters.getOnMessageThreadsKeepAlive(),
            transportParameters.getOnMessageThreadsKeepAliveTimeUnit(),
            new SynchronousQueue<>(),
            threadPoolThreadFactory.orElse(new SiaDc09Servers.SiaThreadFactory("SIA-DC-09-onMessage"))
        );

        for (RctDc09 rct : rcts) {
            Collection<SiaDc09Server> server = buildDc09Server(rct, transportParameters, onMessageExecutor, store, globalParameters, frameUnwrapper);
            servers.addAll(server);
        }
        ThreadFactory threadFactory = serverThreadFactory.orElse(
            new SiaThreadFactory("SIA-DC-09-" + SiaDc09Servers.class.getSimpleName())
        );
        if (!servers.isEmpty()) {
            executor = newFixedThreadPool(servers.size(), threadFactory);
        } else {
            executor = newFixedThreadPool(1, threadFactory);
        }
    }

    // Builds all Sia DC-09 servers.
    private Collection<SiaDc09Server> buildDc09Server(final RctDc09 rct,
                                                      final TransportParameters transportParameters,
                                                      final ExecutorService onMessageExecutor,
                                                      final Dc09SptStore store,
                                                      final Dc09GlobalParameters globalParameters,
                                                      final FrameUnwrapper frameUnwrapper) {

        if (rct.getTransport() == RctDc09.Transport.BOTH) {
            return Lists.newArrayList(
                newUdpServer(rct, transportParameters, onMessageExecutor, store, globalParameters, frameUnwrapper),
                newTcpServer(rct, transportParameters, onMessageExecutor, store, globalParameters, frameUnwrapper)
            );
        }

        return Lists.newArrayList(
            rct.getTransport() == RctDc09.Transport.TCP ?
                newTcpServer(rct, transportParameters, onMessageExecutor, store, globalParameters, frameUnwrapper)
                : newUdpServer(rct, transportParameters, onMessageExecutor, store, globalParameters, frameUnwrapper)
        );

    }

    // Creates a new TCP server.
    private SiaDc09Server newTcpServer(final RctDc09 rct,
                                       final TransportParameters transportParameters,
                                       final ExecutorService onMessageExecutor,
                                       final Dc09SptStore store,
                                       final Dc09GlobalParameters globalParameters,
                                       final FrameUnwrapper frameUnwrapper) {
        return new TcpSiaDc09Server(rct, transportParameters, onMessageExecutor, globalParameters, store, frameUnwrapper);
    }

    // Creates a new UDP server.
    private SiaDc09Server newUdpServer(final RctDc09 rct,
                                       final TransportParameters transportParameters,
                                       final ExecutorService onMessageExecutor,
                                       final Dc09SptStore store,
                                       final Dc09GlobalParameters globalParameters,
                                       final FrameUnwrapper frameUnwrapper) {
        return new UdpSiaDc09Server(rct, transportParameters, onMessageExecutor, globalParameters, store, frameUnwrapper);
    }

    @Override
    public SiaDc09Server addMessageListener(final ServerMessageListener listener) {
        for (SiaDc09Server server : servers) {
            server.addMessageListener(listener);
        }
        return this;
    }

    @Override
    public SiaDc09Server removeMessageListener(final ServerMessageListener listener) {
        for (SiaDc09Server server : servers) {
            server.removeMessageListener(listener);
        }
        return this;
    }


    @Override
    public SiaDc09Server setLogBytes(final boolean flag) {
        for (SiaDc09Server server : servers) {
            server.setLogBytes(flag);
        }
        return this;
    }

    @Override
    public SiaDc09Server setLogMessages(final boolean flag) {
        for (SiaDc09Server server : servers) {
            server.setLogMessages(flag);
        }
        return this;
    }

    @Override
    public void run() {
        servers.forEach(executor::submit);
    }

    @Override
    public void close() {
        for (SiaDc09Server server : servers) {
            try {
                server.close();
            } catch (Exception e) {
                LOG.error("Error while closing all SIA-DC09 servers ({}).", e.getMessage(), e);
            }
        }
        executor.shutdownNow();
        onMessageExecutor.shutdownNow();
    }

    /**
     * The {@code SiaThreadFactory} allows to create threads with specific name.
     * <p/>
     * Each thread created by the factory bears the same prefix name followed by an unique number
     * starting from 0.
     */
    public static class SiaThreadFactory implements ThreadFactory {

        private final String name;
        private final AtomicInteger id = new AtomicInteger(0);

        /**
         * Initializes a new {@code SiaThreadFactory} with given name.
         *
         * @param name the prefix to name threads
         */
        public SiaThreadFactory(final String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(final Runnable runnable) {
            String threadName = name + "-" + id.incrementAndGet();
            return new Thread(runnable, threadName);
        }
    }
}
