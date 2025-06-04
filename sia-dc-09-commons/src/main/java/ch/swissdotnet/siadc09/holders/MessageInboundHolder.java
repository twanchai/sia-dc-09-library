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
import ch.swissdotnet.siadc09.messages.Message;

/**
 * The {@code MessageInboundHolder} class holds the incoming connexion parsed messages.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class MessageInboundHolder {

    // The parsed message.
    private final Message message;
    // The message origin.
    private final RemoteAddressResolver address;

    /**
     * Instantiates a new {@code MessageInboundHolder} with given message and origin.
     *
     * @param message the parsed message
     * @param address the message origin
     */
    public MessageInboundHolder(final Message message, final RemoteAddressResolver address) {
        this.message = message;
        this.address = address;
    }

    /**
     * @return the parsed message
     */
    public Message getMessage() {
        return message;
    }

    /**
     * @return the message origin
     */
    public RemoteAddressResolver getAddress() {
        return address;
    }
}
