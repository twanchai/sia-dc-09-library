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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.*;

/**
 * The {@code RctDc09} class represents the RCT in the SIA DC-09 communication.
 * <p/>
 * It is defined by an IP address, a port and transport. Optionally an ID can be used to ease
 * RCT retrieval by high level application.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class RctDc09 {

    // The ID (if none provided ip:port).
    private final String id;
    // The IP on which the server listens.
    private final String ip;
    // The port on which the server listens.
    private final int port;
    // The communication transport.
    private final Transport transport;

    // Instantiates a new RctDc09 with given builder.
    private RctDc09(final Builder builder) {
        this.id = builder.id;
        this.ip = builder.ip;
        this.port = builder.port;
        this.transport = builder.transport;
    }

    /**
     * Creates a new {@code Builder} with given IP, port and transport.
     *
     * @param ip        the IP address (on which the server listens to)
     * @param port      the port (on which the server listens to)
     * @param transport the transport protocol
     *
     * @return a new {@code Builder} with given parameters
     */
    public static RctDc09.Builder newRctDc09(final String ip, final int port, final Transport transport) {
        return new Builder().ip(ip).port(port).transport(transport);
    }

    /**
     * @return the RCT ID
     */
    public String getId() {
        return id;
    }

    /**
     * @return the RCT IP address (on which the server listens to)
     */
    public String getIp() {
        return ip;
    }

    /**
     * @return the RCT port (on which the server listens to)
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the transport protocol
     */
    public Transport getTransport() {
        return transport;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("ip", ip)
            .add("port", port)
            .add("transport", transport)
            .toString();
    }

    @Override
    public int hashCode() {return Objects.hashCode(id, ip, port, transport);}

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final RctDc09 other = (RctDc09) obj;
        return Objects.equal(this.id, other.id)
            && Objects.equal(this.ip, other.ip)
            && Objects.equal(this.port, other.port)
            && Objects.equal(this.transport, other.transport);
    }

    /**
     * The {@code Transport} enumerations lists all IP protocols which can be used for SIA DC-09.
     */
    public enum Transport {
        /**
         * User Datagram Protocol.
         */
        UDP,
        /**
         * Transmission Control Protocol.
         */
        TCP,
        /**
         * UDP + TCP.
         */
        BOTH
    }

    /**
     * The {@code Builder} class is used to build {@code RctDc09}.
     */
    public static final class Builder {

        // Maximum port range to prevent from creating impossible server.
        public static final int MAXIMUM_PORT_RANGE = 65535;
        // The server ID.
        private String id;
        // The server IP.
        private String ip;
        // The server port.
        private int port;
        // The server transport.
        private Transport transport;

        /**
         * Sets the server ID.
         *
         * @param id the ID to set
         *
         * @return the {@code Builder} instance
         */
        public Builder id(final String id) {
            this.id = checkNotNull(id);
            return this;
        }

        /**
         * Sets the server IP (on which the server listens to).
         *
         * @param ip the IP address
         *
         * @return the {@code Builder} instance
         */
        public Builder ip(final String ip) {
            this.ip = checkNotNull(ip);
            return this;
        }

        /**
         * Sets the server port (on which the server listens to).
         *
         * @param port the numeric port (either TCP and/or UDP)
         *
         * @return the {@code Builder} instance
         */
        public Builder port(final int port) {
            checkArgument(port > 0);
            checkArgument(port < MAXIMUM_PORT_RANGE);
            this.port = port;
            return this;
        }

        /**
         * Sets the transport protocol used by the server.
         *
         * @param transport the transport protocol
         *
         * @return the {@code Builder} instance
         */
        public Builder transport(final Transport transport) {
            this.transport = checkNotNull(transport);
            return this;
        }

        /**
         * @return a new {@code RctDc09} instance with given parameters
         */
        public RctDc09 build() {
            return new RctDc09(this);
        }
    }
}
