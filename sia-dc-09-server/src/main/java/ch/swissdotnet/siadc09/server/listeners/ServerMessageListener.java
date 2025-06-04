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
package ch.swissdotnet.siadc09.server.listeners;

import ch.swissdotnet.siadc09.MessageListener;
import ch.swissdotnet.siadc09.RemoteAddressResolver;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.server.RctDc09;
import org.joda.time.DateTime;

/**
 * The {@code Dc09MessageListener} interface define message handling for DC-09 communication as RCT.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 * @see ServerResponseListener
 */
public interface ServerMessageListener extends MessageListener {

    /**
     * Called upon message reception with all information surrounding the DC-09 message.
     * <p/>
     * Gives the reception time, the SPT and RCT involved and a response handler.
     *
     * @param message   the message received
     * @param reception when the message is received as UTC
     * @param spt       the SPT involved in communication
     * @param rct       the RCT involved in communication
     * @param address   the address on which the message is received
     * @param listener  the listener which handles the response
     */
    void onMessage(final Message message,
                   final DateTime reception,
                   final Dc09Spt spt,
                   final RctDc09 rct,
                   final RemoteAddressResolver address,
                   final ServerResponseListener listener);

}
