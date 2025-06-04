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
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedBytes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * The {@code Dc09Utils} class offer utilities functionality to read and write SIA DC-09 messages.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public final class Dc09Utils {

    /**
     * Line feed character as byte.
     */
    public static final byte LF = '\n';
    /**
     * Carrier return character as byte.
     */
    public static final byte CR = '\r';
    /**
     * Zero ASCII character as byte.
     */
    public static final byte ZERO = '0';
    /**
     * Quote ASCII character as byte.
     */
    public static final byte QUOTE = '"';
    /**
     * Star ASCII character as byte.
     */
    public static final byte STAR = '*';
    /**
     * R ASCII character as byte.
     */
    public static final byte R = 'R';
    /**
     * L ASCII character as byte.
     */
    public static final byte L = 'L';
    /**
     * # ASCII character as byte.
     */
    public static final byte DASH = '#';
    /**
     * _ ASCII character as byte.
     */
    public static final byte UNDERSCORE = '_';
    /**
     * [ ASCII character as byte.
     */
    public static final byte BRACKET_OPEN = '[';
    /**
     * ] ASCII character as byte.
     */
    public static final byte BRACKET_CLOSE = ']';
    /**
     * | ASCII character as byte.
     */
    public static final byte PIPE = '|';
    /**
     * A ASCII character as byte.
     */
    public static final byte A = 'A';
    /**
     * NAK message static data (improve NAK read/write performance).
     */
    public static final String NAK_STATIC_DATA = "R0L0A0[]";
    /**
     * NAK message static data length.
     */
    public static final int NAK_STATIC_DATA_LENGTH = NAK_STATIC_DATA.length();
    /**
     * Base 64 encoder/decoder.
     */
    public static final BaseEncoding BASE_64 = BaseEncoding.base64();
    /**
     * Hexadecimal encoder/decoder.
     */
    public static final BaseEncoding BASE_16 = BaseEncoding.base16();
    /**
     * Hexadecimal lowercase encoder/decoder.
     */
    public static final BaseEncoding BASE_16_LOWER = BaseEncoding.base16().lowerCase();
    /**
     * Maximum bytes to peek at to detect CRC type (CRC + Length + ").
     */
    public static final int MAXIMUM_PEEK_QUOTE = 4 + 4 + 1;
    /**
     * NAK message static data byte content.
     */
    static final byte[] NAK_STATIC_DATA_BYTES = NAK_STATIC_DATA.getBytes(StandardCharsets.UTF_8);
    /**
     * Byte-to-char lookup table.
     */
    private static final char[] BYTE2CHAR = new char[256];
    /**
     * Byte-to-hexadecimal lookup table.
     */
    private static final String[] BYTE2HEX = new String[256];
    /**
     * Byte-to-hexadecimal padded lookup table.
     */
    private static final String[] BYTE2HEX_PAD = new String[256];

    // Shamelessly taken from LoggingHandler and StringUtil from Netty.
    static {
        int i;
        // Generate the lookup table that converts a byte into a 2-digit hexadecimal integer.
        // From 0x00 to 0x09.
        for (i = 0; i < 10; i++) {
            BYTE2HEX_PAD[i] = "0" + i;
        }
        // From 0x0A to 0x0F.
        for (; i < 16; i++) {
            char c = (char) ('a' + i - 10);
            BYTE2HEX_PAD[i] = "0" + c;
        }
        // The remaining (0x10 - 0xFF).
        for (; i < BYTE2HEX_PAD.length; i++) {
            String str = Integer.toHexString(i);
            BYTE2HEX_PAD[i] = str;
        }
        // Generate the lookup table for byte-to-char conversion.
        for (i = 0; i < BYTE2CHAR.length; i++) {
            if (i <= 0x1f || i >= 0x7f) {
                BYTE2CHAR[i] = '.';
            } else {
                BYTE2CHAR[i] = (char) i;
            }
        }
        // Generate the lookup table for byte-to-hex-dump conversion.
        for (i = 0; i < BYTE2HEX.length; i++) {
            BYTE2HEX[i] = ' ' + byteToHexStringPadded(i);
        }
    }

    /**
     * Copies given bytes to a new byte array.
     *
     * @param src the bytes to copy
     *
     * @return the copied array
     */
    public static byte[] copy(final byte[] src) {
        byte[] dest = new byte[src.length];
        System.arraycopy(src, 0, dest, 0, src.length);
        return dest;
    }

    /**
     * Converts the specified byte value into a 2-digit hexadecimal integer.
     *
     * @param value the byte value to convert into 2-digit hexadecimal
     *
     * @return the padded hexadecimal {@code String}
     */
    public static String byteToHexStringPadded(final int value) {
        return BYTE2HEX_PAD[value & 0xff];
    }

    /**
     * Converts the specified bytes as ASCII {@code String}.
     *
     * @param content the bytes to convert into ASCII String
     *
     * @return the ASCII {@code String}
     */
    public static String bytesToAsciiString(final byte[] content) {
        StringBuilder sb = new StringBuilder();
        for (final byte value : content) {
            sb.append(BYTE2CHAR[UnsignedBytes.toInt(value)]);
        }
        return sb.toString();
    }

    /**
     * Converts the specified bytes as 2-digit hexadecimal {@code String}.
     *
     * @param content the bytes to convert into 2-digit hexadecimal {@code String}
     *
     * @return the 2-digit hexadecimal {@code String}
     */
    public static String bytesToHexString(final byte[] content) {
        if (content.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (final byte value : content) {
            sb.append(BYTE2HEX[UnsignedBytes.toInt(value)]);
        }
        return sb.substring(1);
    }

    /**
     * Converts the specified 2-digit hexadecimal {@code String} as byte array.
     * <p/>
     * The size must be even and contains only 0-9 and A-F characters. It is advised to use {@code isHexDigit}
     * before any calls to {@code hexStringToBytes}.
     *
     * @param hexString the 2-digit hexadecimal {@code String} to convert
     *
     * @return the byte array from given 2-digit hexadecimal {@code String}
     *
     * @throws IllegalArgumentException when the size not even or the input has invalid characters.
     * @see Dc09Utils#isHexDigit(String)
     */
    public static byte[] hexStringToBytes(final String hexString) {
        int length = hexString.length();
        if ((length % 2) != 0) {
            throw new IllegalArgumentException("A hex string length must be pair.");
        }
        if (length == 0) {
            return new byte[0];
        }
        byte[] data = new byte[length / 2];
        String twoDigit;
        for (int i = 0; i < length; i += 2) {
            twoDigit = "" + hexString.charAt(i) + hexString.charAt(i + 1);
            try {
                data[i / 2] = (byte) Integer.parseInt(twoDigit, 16);
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Invalid hex string input [" + hexString + "].");
            }
        }
        return data;
    }

    /**
     * Determines whether the given String is a valid hexadecimal {@code String}.
     *
     * @param hexDigit the hexadecimal {@code String} to check
     *
     * @return whether it is a true hex {@code String} or not
     */
    public static boolean isHexDigit(final String hexDigit) {
        char[] hexDigitArray = hexDigit.toCharArray();
        for (final char value : hexDigitArray) {
            if (Character.digit(value, 16) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether the given String is a valid 2-digit hexadecimal {@code String} and its length is even.
     *
     * @param hexDigit the 2-digit hexadecimal {@code String} to check
     *
     * @return whether it is a true 2-digit hex {@code String} or not
     */
    public static boolean is2HexDigit(final String hexDigit) {
        return isHexDigit(hexDigit) && (hexDigit.length() % 2) == 0;
    }

    /**
     * Wraps given byte array as {@code String}.
     *
     * @param value the byte array to wrap
     *
     * @return the {@code String} byte array representation
     */
    public static String asString(final byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    /**
     * Converts given hexadecimal bytes as integer.
     *
     * @param bytes the hexadecimal bytes to convert
     *
     * @return the converted hexadecimal bytes as integer
     *
     * @throws InvalidMessageException when the bytes are not valid hexadecimal value
     */
    public static int hexBytesToInt(final byte[] bytes) throws InvalidMessageException {
        try {
            String value = new String(bytes, StandardCharsets.UTF_8);
            return Integer.parseInt(value, 16);
        } catch (final NumberFormatException e) {
            throw new InvalidMessageException("Invalid message: the provided hexadecimal is invalid.");
        }
    }

    /**
     * Returns the hex value from given integer value on 4 characters.
     * <p/>
     * Similar to {@code Integer#toHexString(int)} but with padding.
     *
     * @param value the value to format to hexadecimal
     *
     * @return the formatted value to hexadecimal
     *
     * @see Integer#toHexString(int)
     */
    public static String intToHexString(final int value) {
        return String.format("%04x", value);
    }

    /**
     * Increments the {@code ByteArrayInputStream} position while attesting its value correspond to given value.
     * <p/>
     * Consumes the {@code ByteArrayInputStream}.
     *
     * @param bais          the stream to read value from
     * @param value         the value to match from read value
     * @param accountNumber the optionally present account number used for Exception data enhancement
     *
     * @throws InvalidMessageException when either the stream is finished or the read and matching value differs
     */
    public static void incrementAttestingValue(final ByteArrayInputStream bais, final byte value, final Optional<String> accountNumber)
        throws InvalidMessageException {
        byte read;
        read = (byte) bais.read();
        if (read == -1) {
            throw new InvalidMessageException(accountNumber, "Invalid message format: expecting [" + (char) value + "] but data finished.");
        }
        if (read != value) {
            throw new InvalidMessageException(accountNumber, "Invalid message format: expecting [" + (char) value + "] read [" + (char) read + "].");
        }
    }

    /**
     * Read single byte from {@code ByteArrayInputStream}.
     * <p/>
     * Consumes the {@code ByteArrayInputStream}.
     *
     * @param bais          the stream to read from
     * @param accountNumber the optionally present account number used for Exception data enhancement
     *
     * @return the byte read
     *
     * @throws InvalidMessageException when the {@code ByteArrayInputStream} is finished
     */
    public static byte readByte(final ByteArrayInputStream bais, final Optional<String> accountNumber) throws InvalidMessageException {
        int read = bais.read();
        if (read == -1) {
            throw new InvalidMessageException(accountNumber, "Invalid message format: the message was finished.");
        }
        return (byte) read;
    }

    /**
     * Extract specified byte number from {@code ByteArrayInputStream}.
     * <p/>
     * Consumes the {@code ByteArrayInputStream}.
     *
     * @param bais          the stream to read bytes from
     * @param size          the byte number to read
     * @param accountNumber the optionally present account number used for Exception data enhancement
     *
     * @return the extracted bytes from {@code ByteArrayInputStream}
     *
     * @throws InvalidMessageException when the {@code ByteArrayInputStream} is finished.
     */
    public static byte[] readBytes(final ByteArrayInputStream bais, final int size, final Optional<String> accountNumber)
        throws InvalidMessageException {

        byte[] bytes = new byte[size];
        try {
            for (int index = 0; index < size; ++index) {
                bytes[index] = readByte(bais, accountNumber);
            }
        } catch (final IllegalStateException e) {
            throw new InvalidMessageException(accountNumber, "Invalid message format: expecting to read [" + size + "] but message was finished.");
        }
        return bytes;
    }

    /**
     * Reads the {@code ByteArrayInputStream} until reaching a specified value (not included) or reaching a limit.
     * <p/>
     * Consumes the {@code ByteArrayInputStream}.
     *
     * @param bais          the stream to read bytes from
     * @param value         the value to read until (not included)
     * @param max           the maximum byte number to read
     * @param accountNumber the optionally present account number used for Exception data enhancement
     *
     * @return the extracted bytes from {@code ByteArrayInputStream}
     *
     * @throws InvalidMessageException when the value has not been found in max tries
     */
    public static byte[] readUntil(final ByteArrayInputStream bais, final byte value, final int max, final Optional<String> accountNumber)
        throws InvalidMessageException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int current;
        for (current = 0; current < max && peek(bais) != value; ++current) {
            baos.write(readByte(bais, accountNumber));
        }
        if (current == max) {
            throw new InvalidMessageException(accountNumber, "Invalid message: Value [" + (char) value + "] after [" + max + "] tries.");
        }
        return baos.toByteArray();
    }

    /**
     * Finds the first index of specified value or -1 when unable to find it until reaching the maximum.
     * <p/>
     * Does <u>NOT</u> consume the {@code ByteArrayInputStream}.
     *
     * @param bais  the stream to read from
     * @param value the value to get the first index of
     * @param max   the maximal byte number to read
     *
     * @return the first index of value detected or -1 if not detected
     */
    public static int indexOf(final ByteArrayInputStream bais, final byte value, final int max) {
        bais.mark(0);
        int count = 0;
        byte read;
        do {
            read = (byte) bais.read();
        } while ((read != value) && (++count < max));
        bais.reset();
        return (count < max) ? count : -1;
    }

    /**
     * Peek the next byte from {@code ByteArrayInputStream}.
     * <p/>
     * Does <u>NOT</u> consume the {@code ByteArrayInputStream}.
     *
     * @param bais the stream to peek from
     *
     * @return the peeked byte
     */
    public static byte peek(final ByteArrayInputStream bais) {
        bais.mark(0);
        byte value;
        try {
            value = (byte) bais.read();
        } finally {
            bais.reset();
        }
        return value;
    }

    /**
     * Peek the next given bytes from {@code ByteArrayInputStream}.
     * <p/>
     * Does <u>NOT</u> consume the {@code ByteArrayInputStream}.
     *
     * @param bais          the stream to peek from
     * @param size          the byte number to peek
     * @param accountNumber the optionally present account number used for Exception data enhancement
     *
     * @return the peeked bytes
     *
     * @throws InvalidMessageException when the {@code ByteArrayInputStream} could not be peeked desired size
     */
    public static byte[] peek(final ByteArrayInputStream bais, final int size, final Optional<String> accountNumber) throws InvalidMessageException {
        bais.mark(0);
        byte[] value = new byte[size];
        boolean failed = true;
        try {
            int read = bais.read(value);
            failed = read == -1;
        } catch (IOException ignored) {
        } finally {
            bais.reset();
        }
        if (failed) {
            throw new InvalidMessageException(accountNumber, "Invalid message: could not peek [" + size + "] bytes.");
        }
        return value;
    }

}
