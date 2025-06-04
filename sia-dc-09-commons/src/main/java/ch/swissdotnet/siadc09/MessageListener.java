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

import ch.swissdotnet.siadc09.exceptions.Dc09Exception;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;

import java.util.Optional;


public interface MessageListener {

    /**
     * Called upon {@code Exception} reception during DC-09 exchange.
     *
     * @param exception the received exception wrapped in a {@code Dc09Exception}
     * @param address   the address on which the bytes are received
     */
    void onError(final Dc09Exception exception, final RemoteAddressResolver address);

    /**
     * Called upon byte reception or distribution during DC-09 exchange.
     *
     * @param message   the bytes received or sent
     * @param address   the address on which the bytes are received
     * @param sptDc09   the optionally present SPT involved in communication
     * @param direction whether the bytes are received or sent
     */
    void onByteLog(final byte[] message,
                   final RemoteAddressResolver address,
                   final Optional<Dc09Spt> sptDc09,
                   final Direction direction);

    /**
     * Called upon message reception or distribution during DC-09 exchange.
     *
     * @param message   the message received or sent
     * @param address   the address on which the message is received
     * @param sptDc09   the optionally present SPT involved in communication
     * @param direction whether the message is received or sent
     */
    void onMessageLog(final Message message,
                      final RemoteAddressResolver address,
                      final Optional<Dc09Spt> sptDc09,
                      final Direction direction);

    /**
     * The {@code Direction} enumeration lists communication direction for log messages.
     */
    enum Direction {
        /**
         * The content is received from SPT.
         */
        INCOMING,
        /**
         * The content is going to the SPT.
         */
        OUTGOING
    }

}
