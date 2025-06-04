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

import ch.swissdotnet.siadc09.Dc09Utils;
import ch.swissdotnet.siadc09.messages.crc.Crc16;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.nio.charset.StandardCharsets;

/**
 * The {@code StandardDc09} class defines all standard message sizes and positions.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class StandardDc09 implements Dc09Version {

    // Standard format: HH:MM:SS,MM-DD-YYYY.
    private static final DateTimeFormatter STANDARD_PARSER =
        new DateTimeFormatterBuilder()
            .appendHourOfDay(2)
            .appendLiteral(':')
            .appendMinuteOfHour(2)
            .appendLiteral(':')
            .appendSecondOfMinute(2)
            .appendLiteral(',')
            .appendMonthOfYear(2)
            .appendLiteral('-')
            .appendDayOfMonth(2)
            .appendLiteral('-')
            .appendYear(4, 4)
            .toFormatter();

    @Override
    public int lfSize() {
        return 1;
    }

    @Override
    public int crcSize() {
        return 4;
    }

    @Override
    public int crcPos() {
        return lfSize();
    }

    @Override
    public int lengthSize() {
        return 3;
    }

    @Override
    public int lengthPos() {
        return lfSize() + crcSize();
    }

    @Override
    public int headerSize() {
        return lfSize() + crcSize() + lengthSize();
    }

    @Override
    public int seqSize() {
        return 4;
    }

    @Override
    public int messageTokenIdSize() {
        return 3;
    }

    @Override
    public int maxIdSize() {
        return 10; // " + * + token + "
    }

    @Override
    public int maxSequenceValue() {
        return 9999;
    }

    @Override
    public int maxRcvrSize() {
        return 6; // R + 6
    }

    @Override
    public int maxPrefSize() {
        return 6; // L + 6
    }

    @Override
    public int maxAccSize() {
        return 16; // # + 16
    }

    @Override
    public int maxClearData() {
        //<LF><crc><0LLL><"id"><seq><Rrvcr><Lpref><#acct>[
        return headerSize() + maxIdSize() + seqSize() + maxRcvrSize() + maxPrefSize() + maxAccSize() + 1;
    }

    @Override
    public int encryptedAlignment() {
        return 16;
    }

    @Override
    public int maxPaddingSize() {
        return encryptedAlignment() + 1;
    }

    @Override
    public int timestampSize() {
        return 19; // HH:MM:SS,MM-DD-YYYY.
    }

    @Override
    public DateTimeFormatter timestampFormat() {
        return STANDARD_PARSER;
    }

    @Override
    public int maxMessageLength() {
        return 0xFFF;
    }

    @Override
    public byte[] crc(final byte[] middle, final boolean hexToUpper) {
        int crcValue = Crc16.calculateCrc(middle);
        String hexString = Dc09Utils.intToHexString(crcValue);
        if (hexToUpper) {
            hexString = hexString.toUpperCase();
        }
        return hexString.getBytes(StandardCharsets.UTF_8);
    }

}
