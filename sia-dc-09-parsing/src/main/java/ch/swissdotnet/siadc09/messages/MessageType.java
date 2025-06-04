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
package ch.swissdotnet.siadc09.messages;

import java.nio.charset.StandardCharsets;

/**
 * The {@code MessageType} enumeration lists all SIA DC-09 messages.
 * <p/>
 * It stores whether a message may be ciphered, whether it contains data and whether it may contain additional
 * data or not.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public enum MessageType {
    /**
     * The SPT to RCT event message which may be ciphered, contains data and may contain additional data.
     */
    EVENT(true, true, true),
    /**
     * The RCT to SPT acknowledge message which may be ciphered, contains no data and no additional data.
     */
    ACK(true, false, false),
    /**
     * The RCT to SPT negative acknowledgement message which is <u>NEVER</u> ciphered, contains no data and no additional data.
     */
    NAK(false, false, false),
    /**
     * The RCT to SPT response message which may be ciphered, contains data and may contain additional data.
     */
    RSP(true, true, true),
    /**
     * The RCT to SPT unable acknowledgement message which is <u>NEVER</u> ciphered, contains no data and no additional data.
     */
    DUH(false, false, false),
    /**
     * The SPT to RCT polling message which may be ciphered, contains no data and may contain additional data.
     */
    NULL(true, false, true);

    // Whether the message may be ciphered.
    private final boolean mayBeCiphered;
    // Whether the message contains data or not.
    private final boolean containsData;
    // Whether the message may contain additional data or not.
    private final boolean mayContainAdditionalData;
    // The message type value.
    private final String value;

    MessageType(final boolean mayBeCiphered, final boolean containsData, final boolean mayContainAdditionalData) {
        this.mayBeCiphered = mayBeCiphered;
        this.containsData = containsData;
        this.mayContainAdditionalData = mayContainAdditionalData;
        value = toString();
    }

    /**
     * Tries to get the {@code MessageType} from given byte array or {@code EVENT}.
     *
     * @param value the array byte to get {@code MessageType} from
     *
     * @return the matching {@code MessageType} or {@code EVENT}
     */
    public static MessageType fromByte(final byte[] value) {
        MessageType response = EVENT;
        String valueString = new String(value, StandardCharsets.UTF_8);
        for (MessageType type : values()) {
            if (type.value.equals(valueString)) {
                response = type;
                break;
            }
        }
        return response;
    }

    /**
     * @return whether the message may be ciphered or not
     */
    public boolean mayBeCiphered() {
        return mayBeCiphered;
    }

    /**
     * @return whether the message may contain data or not
     */
    public boolean containsData() {
        return containsData;
    }

    /**
     * @return whether message may contain additional data or not
     */
    public boolean mayContainAdditionalData() {
        return mayContainAdditionalData;
    }
}
