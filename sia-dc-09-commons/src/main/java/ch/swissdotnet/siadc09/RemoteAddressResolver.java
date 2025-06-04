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

import com.google.common.base.MoreObjects;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * The {@code RemoteAddressResolver} class allow access to remote {@code host}, {@code port}
 * and {@code socket address} for any given DC-09 message.
 */
public interface RemoteAddressResolver {

    /**
     * @return the remote {@code host} address
     */
    String getHost();

    /**
     * @return the remote {@code port}
     */
    int getPort();

    /**
     * @return the remote {@code socket address}
     */
    InetSocketAddress getInetSocketAddress();

    /**
     * The {@code InetSocketAddressResolver} class is the standard {@code RemoteAddressResolver} implementation.
     * </p>
     * It is backed up by {@code InetSocketAddress} call forwarding.
     */
    final class InetSocketAddressResolver implements RemoteAddressResolver {

        // The address to forward to.
        private final InetSocketAddress address;

        /**
         * @param address the remote {@code socket address}
         */
        public InetSocketAddressResolver(final InetSocketAddress address) {
            this.address = Objects.requireNonNull(address);
        }

        @Override
        public String getHost() {
            return address.getHostString();
        }

        @Override
        public int getPort() {
            return address.getPort();
        }

        @Override
        public InetSocketAddress getInetSocketAddress() {
            return address;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("address", address)
                .toString();
        }
    }

}
