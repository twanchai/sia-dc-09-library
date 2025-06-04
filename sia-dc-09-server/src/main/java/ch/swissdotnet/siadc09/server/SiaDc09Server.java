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

import ch.swissdotnet.siadc09.server.listeners.ServerMessageListener;

/**
 * The {@code SiaDc09Server} interface defines the SIA DC-09 server functionalities.
 * <p/>
 * A DC-09 server listen to incoming connexion by SPT, decodes messages, dispatches it to {@code Dc09MessageListener}
 * and encodes the response back to SPT.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public interface SiaDc09Server extends Runnable, AutoCloseable {

    /**
     * Adds a {@code Dc09MessageListener} to the server.
     *
     * @param listener the listener to add to the server
     *
     * @return the {@code SiaDc09Server} instance
     */
    SiaDc09Server addMessageListener(final ServerMessageListener listener);

    /**
     * Removes a {@code Dc09MessageListener} from the server.
     *
     * @param listener the listener to remove from the server
     *
     * @return the {@code SiaDc09Server} instance
     */
    SiaDc09Server removeMessageListener(final ServerMessageListener listener);

    /**
     * Sets the server to log or not incoming and outgoing bytes content.
     *
     * @param flag whether to log or not incoming and outgoing bytes content
     *
     * @return the {@code SiaDc09Server} instance
     */
    SiaDc09Server setLogBytes(final boolean flag);

    /**
     * Sets the server to log or not incoming and outgoing messages.
     *
     * @param flag whether to log or not incoming and outgoing messages
     *
     * @return the {@code SiaDc09Server} instance
     */
    SiaDc09Server setLogMessages(final boolean flag);

}
