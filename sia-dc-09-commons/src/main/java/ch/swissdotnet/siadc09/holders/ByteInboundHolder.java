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

import ch.swissdotnet.siadc09.Dc09Utils;
import ch.swissdotnet.siadc09.RemoteAddressResolver;

/**
 * The {@code ByteHolder} class holds byte content and message provenance.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class ByteInboundHolder {

    // Byte content.
    private final byte[] content;
    // Message origin.
    private final RemoteAddressResolver address;

    /**
     * Instantiates a new {@code ByteHolder} with given content and origin.
     *
     * @param content the message content
     * @param address the message origin
     */
    public ByteInboundHolder(final byte[] content, final RemoteAddressResolver address) {
        this.content = Dc09Utils.copy(content);
        this.address = address;
    }

    /**
     * @return the message content
     */
    public byte[] getContent() {
        return Dc09Utils.copy(content);
    }

    /**
     * @return the message origin
     */
    public RemoteAddressResolver getAddress() {
        return address;
    }
}
