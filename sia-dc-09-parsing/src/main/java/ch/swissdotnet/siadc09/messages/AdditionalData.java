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

import com.google.common.base.MoreObjects;

import java.nio.charset.Charset;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * The {@code AdditionalData} class stores additional data for {@link DataMessage} ({@link Event} and {@link Polling}).
 * <p/>
 * It has a type and a {@code String} content.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class AdditionalData {

    // Windows 1252 charset.
    public static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    // Additional data type.
    private final AdditionalDataType type;
    // Additional data content.
    private final String content;

    /**
     * Instantiates a new {@code AdditionalData} with given type and content.
     *
     * @param type    the {@code AdditionalData} type
     * @param content the content associated with type
     */
    public AdditionalData(final AdditionalDataType type, final String content) {
        this.type = type;
        this.content = content;
    }

    /**
     * @return the {@code AdditionalData} type
     */
    public AdditionalDataType getType() {
        return type;
    }

    /**
     * @return the content associated with type
     */
    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .omitNullValues()
            .add("type", type)
            .add("content", content)
            .toString();
    }

    /**
     * The {@code AdditionalDataType} enumeration lists all Extended Data Identifiers
     * presented in "ANSI/SIA DC-09 2013" on page 19.
     * <p/>
     * Their content associated with additional data type must be evaluated based on
     * the given norm.
     */
    public enum AdditionalDataType {
        AUTHENTICATION_HASH('A', US_ASCII),
        OCCURRENCE('H', US_ASCII),
        MAC_ADDRESS('M', US_ASCII),
        VERIFICATION('V', WINDOWS_1252),
        PROGRAMMING_DATA('P', WINDOWS_1252),
        ALARM_TEXT('I', WINDOWS_1252),
        SITE_NAME('S', WINDOWS_1252),
        BUILDING_NAME('O', WINDOWS_1252),
        LOCATION('L', WINDOWS_1252),
        ROOM('R', WINDOWS_1252),
        ALARM_TRIGGER('T', US_ASCII),
        LONGITUDE('X', US_ASCII),
        LATITUDE('Y', US_ASCII),
        ALTITUDE('Z', US_ASCII),
        UNKNOWN('-', US_ASCII);

        // The type identifier.
        private final char identifier;
        // The charset used by the additional data type.
        private final Charset charset;

        AdditionalDataType(final char identifier, final Charset charset) {
            this.identifier = identifier;
            this.charset = Optional.ofNullable(charset).orElse(US_ASCII);
        }

        /**
         * Tries to get the {@code AdditionalDataType} based on given identifier or {@code UNKNOWN}.
         *
         * @param identifier the data type identifier
         *
         * @return the {@code AdditionalDataType} associated with identifier or {@code UNKNOWN}
         */
        public static AdditionalDataType forIdentifier(final char identifier) {
            AdditionalDataType type = UNKNOWN;
            for (AdditionalDataType value : AdditionalDataType.values()) {
                if (value.identifier == identifier) {
                    type = value;
                    break;
                }
            }
            return type;
        }

        /**
         * @return the data type identifier
         */
        public char getIdentifier() {
            return identifier;
        }

        /**
         * @return the charset used by this identifier
         */
        public Charset getCharset() {
            return charset;
        }
    }

}
