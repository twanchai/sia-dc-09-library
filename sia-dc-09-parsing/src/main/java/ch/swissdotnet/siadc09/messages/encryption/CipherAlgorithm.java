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
package ch.swissdotnet.siadc09.messages.encryption;

import ch.swissdotnet.siadc09.exceptions.InvalidCipheringException;
import ch.swissdotnet.siadc09.messages.versions.Dc09Version;

/**
 * The {@code CipherAlgorithm} interface defines how ciphering algorithm cipher and decipher data for
 * SIA DC-09 messages.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public interface CipherAlgorithm {

    /**
     * AES Algorithm use to cipher/decipher SIA DC-09 messages.
     */
    String AES_ALGORITHM = "AES";

    /**
     * Ciphers given clear array byte with given SIA DC-09 version.
     * <p/>
     * Pads data to have aligned block size.
     *
     * @param clear   the data to cipher
     * @param version the SIA DC-09 version to cipher for
     *
     * @return the ciphered data with padded content leading the ciphered data
     *
     * @throws InvalidCipheringException when the ciphering is not possible for given data
     */
    byte[] cipher(final byte[] clear, final Dc09Version version) throws InvalidCipheringException;

    /**
     * Deciphers given ciphered data with underlying algorithm.
     *
     * @param cipherData the data to decipher
     *
     * @return the deciphered data
     *
     * @throws InvalidCipheringException when the deciphering is not possible for given data
     */
    byte[] decipher(byte[] cipherData) throws InvalidCipheringException;

}
