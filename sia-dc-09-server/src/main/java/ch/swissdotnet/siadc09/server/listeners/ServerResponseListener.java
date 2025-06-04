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

import ch.swissdotnet.siadc09.messages.Message;

/**
 * The {@code Dc09ResponseListener} interface allows {@code Dc09MessageListener} to either give response to
 * SPT or close the connexion.
 * <p/>
 * All {@code Dc09MessageListener} can call its {@code Dc09ResponseListener}, the first response given being the
 * only sent.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 * @see ServerMessageListener
 */
public interface ServerResponseListener {

    /**
     * Give response to the {@code SPT} with given message.
     *
     * @param message the message to send back
     */
    void response(final Message message);

    /**
     * Closes the communication with SPT, gives no response.
     */
    void close();

}
