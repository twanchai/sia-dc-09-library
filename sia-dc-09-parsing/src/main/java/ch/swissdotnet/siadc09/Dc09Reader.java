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

import ch.swissdotnet.siadc09.exceptions.InvalidMessageException;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;

/**
 * The {@code Dc09Reader} interface defines how ANSI/SIA DC-09 messages should be read from given byte array.
 * <p/>
 * As the reading process involve determining the SPT parameters, a {@code Dc09SptStore} is used to
 * retrieve cipher/decipher mechanism.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public interface Dc09Reader {

    /**
     * Reads given byte array with global parameters and SPT store.
     * <p/>
     * Outputs a {@code Message} with read parameters.
     *
     * @param data   the data to read from
     * @param global the global read parameters
     * @param store  the store to retrieve SPT from
     *
     * @return the {@code Message} corresponding to read message
     *
     * @throws InvalidMessageException when an error occurs during message parsing
     */
    Message read(final byte[] data,
                 final Dc09GlobalParameters global,
                 final Dc09SptStore store) throws InvalidMessageException;

}
