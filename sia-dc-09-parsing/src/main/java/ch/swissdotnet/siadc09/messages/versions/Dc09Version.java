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
package ch.swissdotnet.siadc09.messages.versions;

import org.joda.time.format.DateTimeFormatter;


/**
 * The {@code Dc09Version} defines SIA DC-09 message structure sizes, position and computation.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public interface Dc09Version {

    /**
     * @return the Line feed size
     */
    int lfSize();

    /**
     * @return the CRC size
     */
    int crcSize();

    /**
     * @return the CRC starting position
     */
    int crcPos();

    /**
     * @return the length size
     */
    int lengthSize();

    /**
     * @return the length starting position
     */
    int lengthPos();

    /**
     * @return the header size (LF + CRC + Length)
     */
    int headerSize();

    /**
     * @return the sequence size
     */
    int seqSize();

    /**
     * @return the token ID size (ACK, NAK, ...)
     */
    int messageTokenIdSize();

    /**
     * @return the maximum ID size ("SIA-DCS", "*ADM-CID", ...)
     */
    int maxIdSize();

    /**
     * @return the maximum sequence value
     */
    int maxSequenceValue();

    /**
     * @return the maximum receiver number size
     */
    int maxRcvrSize();

    /**
     * @return the maximum account prefix size
     */
    int maxPrefSize();

    /**
     * @return the maximum account number size
     */
    int maxAccSize();

    /**
     * @return the maximum cleared data size
     */
    int maxClearData();

    /**
     * @return the encrypted alignment used for ciphering/deciphering
     */
    int encryptedAlignment();

    /**
     * @return the maximum padding size
     */
    int maxPaddingSize();

    /**
     * @return the timestamp size
     */
    int timestampSize();

    /**
     * @return the timestamp parser/formatter
     */
    DateTimeFormatter timestampFormat();

    /**
     * @return the maximum message length
     */
    int maxMessageLength();

    /**
     * Computes the CRC for given message.
     *
     * @param middle     the middle part of the message (between header and carriage return)
     * @param hexToUpper whether the CRC value is to hexadecimal uppercase or not
     *
     * @return the computed CRC value for given message
     */
    byte[] crc(final byte[] middle, final boolean hexToUpper);

    /**
     * The {@code Version} enumeration lists all SIA DC-09 version supported.
     * <p/>
     * A version is a minor implementation which deviates from the norm due to misread or
     * manufacturer errors.
     */
    enum Version {
        /**
         * The standard implementation based on norm.
         */
        STANDARD,
        /**
         * The implementation which deviates from standard by having its CRC as binary
         * as opposed to ASCII hexadecimal.
         */
        BINARY_CRC
    }

}
