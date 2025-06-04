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
import ch.swissdotnet.siadc09.parameters.Dc09SptParameters;

/**
 * The {@code Dc09Writer} interface defines how SIA DC-09 messages are serialized.
 * <p/>
 * Depending on whether the message is ciphered or not, it may use given {@code Dc09SptParameters} to cipher content.*
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public interface Dc09Writer {

    /**
     * Serializes given {@code Message} as a byte array with given global and SPT parameters.
     *
     * @param message the message to serialize
     * @param global  the global parameters used to serialize message
     * @param spt     the spt parameters used to serialize message
     *
     * @return the {@code SerializedMessage} associated with {@code Message} serialization
     *
     * @throws InvalidMessageException when an error occurs during message serialization
     */
    SerializedMessage write(final Message message,
                            final Dc09GlobalParameters global,
                            final Dc09SptParameters spt) throws InvalidMessageException;

    /**
     * The {@code SerializedMessage} class holds both message and clear data (data before cipher).
     */
    final class SerializedMessage {

        // Full message
        private final byte[] message;
        // Clear data contained in message after "[" character.
        private final byte[] clearData;

        /**
         * Initializes a new {@code SerializedMessage} with given content.
         *
         * @param message   the serialized message
         * @param clearData the serialized clear data
         */
        public SerializedMessage(final byte[] message, final byte[] clearData) {
            this.message = Dc09Utils.copy(message);
            this.clearData = Dc09Utils.copy(clearData);
        }

        /**
         * @return the whole message serialized
         */
        public byte[] message() {
            return Dc09Utils.copy(message);
        }

        /**
         * @return the clear data present in message
         */
        public byte[] clearData() {
            return Dc09Utils.copy(clearData);
        }
    }

}
