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
package ch.swissdotnet.siadc09.holders;

import ch.swissdotnet.siadc09.RemoteAddressResolver;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;


public final class ByteOutboundHolder extends ByteInboundHolder {

    // The concerned SPT
    private final Dc09Spt spt;

    /**
     * Instantiates a new {@code ByteHolder} with given content, origin and associated SPT.
     *
     * @param content the message content
     * @param address the message origin
     * @param spt     the SPT concerned by the message
     */
    public ByteOutboundHolder(final byte[] content, final RemoteAddressResolver address, final Dc09Spt spt) {
        super(content, address);
        this.spt = spt;
    }

    /**
     * @return the SPT associated with the message
     */
    public Dc09Spt getSpt() {
        return spt;
    }
}
