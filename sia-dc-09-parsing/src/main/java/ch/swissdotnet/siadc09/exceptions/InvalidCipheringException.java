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
package ch.swissdotnet.siadc09.exceptions;

/**
 * The {@code InvalidCipheringException} class may be thrown when creating and using SPT cipher algorithm.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 * @see ch.swissdotnet.siadc09.messages.encryption.CipherAlgorithm
 */
public class InvalidCipheringException extends Exception {

    /**
     * Instantiate a new {@code InvalidCipheringException}.
     */
    public InvalidCipheringException() {
        super();
    }

    /**
     * Instantiate a new {@code InvalidCipheringException} with given message.
     *
     * @param message the message to wrap exception with
     */
    public InvalidCipheringException(final String message) {
        super(message);
    }

}
