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
package ch.swissdotnet.siadc09.tests;

import ch.swissdotnet.siadc09.Dc09Handler;
import ch.swissdotnet.siadc09.Dc09Utils;
import ch.swissdotnet.siadc09.exceptions.InvalidCipheringException;
import ch.swissdotnet.siadc09.exceptions.InvalidMessageException;
import ch.swissdotnet.siadc09.messages.AdditionalData;
import ch.swissdotnet.siadc09.messages.DataMessage;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.messages.MessageType;
import ch.swissdotnet.siadc09.messages.encryption.AesCbcCipherAlgorithm;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.parameters.Dc09SptParameters;
import com.google.common.primitives.UnsignedBytes;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ParsingBinaryCrcTest {

    protected Dc09GlobalParameters globalParameters = new Dc09GlobalParameters();
    protected Dc09TestSptStore sptStore = new Dc09TestSptStore();

    private static void validate(final Message message,
                                 final MessageType type,
                                 final String crc,
                                 final int size,
                                 final int sequence,
                                 final String accountPrefix,
                                 final String accountNumber) {

        Assertions.assertEquals(type, message.type());
        Assertions.assertEquals(crc, new String(message.getReadCrc()));
        Assertions.assertArrayEquals(message.getReadCrc(), message.getExpectedCrc());
        Assertions.assertEquals(size, message.getReadLength());
        Assertions.assertEquals(message.getReadLength(), message.getActualLength());
        Assertions.assertEquals(sequence, message.getSequence());
        Assertions.assertEquals(accountPrefix, message.getAccountPrefix());
        Assertions.assertEquals(accountNumber, message.getAccountNumber());
    }

    @Test
    public void parseAckCorrectlyHexUpper() throws InvalidMessageException {
        byte[] crc = {UnsignedBytes.checkedCast('\''), UnsignedBytes.checkedCast('\u0029')};
        byte[] value = "\n  002e\"ACK\"0001L0#080027E62A64[]_16:07:07,04-14-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(16)
            .withMinuteOfHour(7)
            .withSecondOfMinute(7)
            .withMillisOfSecond(0)
            .withDayOfMonth(14)
            .withMonthOfYear(4)
            .withYear(2015);

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertFalse(message.getReceiverNumber().isPresent());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.ACK, new String(crc), 46, 1, "0", "080027E62A64");
    }

    @Test
    public void parseAckCorrectlyHexLower() throws InvalidMessageException {
        byte[] crc = {UnsignedBytes.checkedCast('\u0001'), UnsignedBytes.checkedCast('\u00fc')};
        byte[] value = "\n  0037\"ACK\"0000Rc0ffdL1234#080027E62A64[]_12:10:40,06-22-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(12)
            .withMinuteOfHour(10)
            .withSecondOfMinute(40)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("c0ffd", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.ACK, new String(crc), 55, 0, "1234", "080027E62A64");
    }

    @Test
    public void parseLengthInvalid() {
        String value = "\n01fc003g\"ACK\"0000Rc0ffdL1234#080027E62A64[]_12:10:40,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();
        Assertions.assertThrows(InvalidMessageException.class, () -> handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore));
    }

    @Test
    public void parseAckCorrectlyWithPrefixAndReceiverNumber() throws InvalidMessageException {
        byte[] crc = {UnsignedBytes.checkedCast('\u0041'), UnsignedBytes.checkedCast('\u006A')};
        byte[] value = "\n  0036\"ACK\"0001R5678L1234#080027E62A64[]_16:07:53,04-14-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(16)
            .withMinuteOfHour(7)
            .withSecondOfMinute(53)
            .withMillisOfSecond(0)
            .withDayOfMonth(14)
            .withMonthOfYear(4)
            .withYear(2015);

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("5678", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.ACK, new String(crc), 54, 1, "1234", "080027E62A64");
    }

    @Test
    public void parseAckCorrectlyWithPrefixAndReceiverNumberCipheredWithExceptionWrongKey() throws InvalidCipheringException {

        byte[] value = "\n  0062\"*ACK\"0001R5678L1234#080027E62A64[14F088FF5DCB19D1908069507EAB97C7CDB20F1C6EFA550BB59864D54B2DFA1C\r".getBytes();

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCE");
        sptStore.parameters.put(
            "080027E62A64",
            Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build()
        );
        Dc09Handler handler = new Dc09Handler();
        Assertions.assertThrows(InvalidMessageException.class, () -> handler.read(value, globalParameters, sptStore));
    }

    @Test
    public void parseAckCorrectlyWithPrefixAndReceiverNumberCiphered1() throws InvalidMessageException, InvalidCipheringException {

        byte[] crc = {UnsignedBytes.checkedCast('\u00e3'), UnsignedBytes.checkedCast('\u004c')};
        byte[] value = "\n  0062\"*ACK\"0001R5678L1234#080027E62A64[14F088FF5DCB19D1908069507EAB97C7CDB20F1C6EFA550BB59864D54B2DFA1C\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptStore.parameters.put(
            "080027E62A64",
            Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build()
        );
        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(16)
            .withMinuteOfHour(8)
            .withSecondOfMinute(12)
            .withMillisOfSecond(0)
            .withDayOfMonth(14)
            .withMonthOfYear(4)
            .withYear(2015);

        Assertions.assertTrue(message.isValid());
        Assertions.assertTrue(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("5678", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.ACK, new String(crc), 98, 1, "1234", "080027E62A64");
    }

    @Test
    public void parseAckCorrectlyWithPrefixAndReceiverNumberCiphered2() throws InvalidMessageException, InvalidCipheringException {

        byte[] crc = {UnsignedBytes.checkedCast('\u0002'), UnsignedBytes.checkedCast('\u00b7')};
        byte[] value = "\n  0063\"*ACK\"0000RC0FFDL1234#00900B376AB6[5F1BEAF37775DEC5F901894BDD25D9565C122B2BBAAD72E84E23B8F75554EF8C\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        byte[] hexKey = Dc09Utils.hexStringToBytes("28376EF26E782E1A4A160E3276E5FB93");
        sptStore.parameters.put(
            "00900B376AB6",
            Dc09Spt.newSptBuilder("00900B376AB6", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build()
        );
        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(8)
            .withMinuteOfHour(6)
            .withSecondOfMinute(26)
            .withMillisOfSecond(0)
            .withDayOfMonth(27)
            .withMonthOfYear(4)
            .withYear(2015);

        Assertions.assertTrue(message.isValid());
        Assertions.assertTrue(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.ACK, new String(crc), 99, 0, "1234", "00900B376AB6");
    }

    @Test
    public void parseNakCorrectly() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u003c'), UnsignedBytes.checkedCast('\u0083')};
        byte[] value = "\n  0025\"NAK\"0000R0L0A0[]_16:09:09,04-14-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(16)
            .withMinuteOfHour(9)
            .withSecondOfMinute(9)
            .withMillisOfSecond(0)
            .withDayOfMonth(14)
            .withMonthOfYear(4)
            .withYear(2015);

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("0", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.NAK, new String(crc), 37, 0, "0", "0");
    }

    @Test
    public void parseNakInvalid() {

        String[] values = {"\n3C830025\"NAK\"0000R1L0A0[]_16:09:09,04-14-2015\r", "\n3C830025\"NAK\"0000R0L1A0[]_16:09:09,04-14-2015\r", "\n3C830025\"NAK\"0000R0L0A1[]_16:09:09,04-14-2015\r",};

        Dc09Handler handler = new Dc09Handler();
        for (final String value : values) {
            try {
                handler.read(value.getBytes(), globalParameters, sptStore);
            } catch (InvalidMessageException ignored) {
                Assertions.fail("Message [" + value + "] should not have failed.");
            }
        }

    }

    @Test
    public void parsePollingStandard1() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u00c2'), UnsignedBytes.checkedCast('\u006e')};
        byte[] value = "\n  0038\"NULL\"0000RC0FFDL1234#080027E62A64[]_13:40:27,06-22-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(13)
            .withMinuteOfHour(40)
            .withSecondOfMinute(27)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.NULL, new String(crc), 56, 0, "1234", "080027E62A64");
    }

    @Test
    public void parsePollingStandard2() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u008a'), UnsignedBytes.checkedCast('\u0059')};
        byte[] value = "\n  0038\"NULL\"0000Rc0ffdL1234#080027E62A64[]_15:50:24,06-22-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(50)
            .withSecondOfMinute(24)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertFalse(message.getPadding().isPresent());

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("c0ffd", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.NULL, new String(crc), 56, 0, "1234", "080027E62A64");

    }

    @Test
    public void parsePollingStandard3() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u0053'), UnsignedBytes.checkedCast('\u003f')};
        byte[] value = "\n  0038\"NULL\"0000RC0FFDL1234#080027E62A64[]_15:52:00,06-22-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(52)
            .withSecondOfMinute(0)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertFalse(message.getPadding().isPresent());

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.NULL, new String(crc), 56, 0, "1234", "080027E62A64");

    }

    @Test
    public void parsePollingStandardNoTimestamp1() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u0052'), UnsignedBytes.checkedCast('\u005a')};
        byte[] value = "\n  0024\"NULL\"0000Rc0ffdL1234#080027E62A64[]\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        Assertions.assertFalse(message.getPadding().isPresent());

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("c0ffd", message.getReceiverNumber().get());
        Assertions.assertFalse(message.getTimestamp().isPresent());
        validate(message, MessageType.NULL, new String(crc), 36, 0, "1234", "080027E62A64");

    }

    @Test
    public void parsePollingStandardNoTimestamp2() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u0059'), UnsignedBytes.checkedCast('\u006e')};
        byte[] value = "\n  0024\"NULL\"0000RC0FFDL1234#080027E62A64[]\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        Assertions.assertFalse(message.getPadding().isPresent());

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertFalse(message.getTimestamp().isPresent());
        validate(message, MessageType.NULL, new String(crc), 36, 0, "1234", "080027E62A64");

    }

    @Test
    public void parsePollingCiphered() throws InvalidMessageException, InvalidCipheringException {

        byte[] crc = {UnsignedBytes.checkedCast('\u00ca'), UnsignedBytes.checkedCast('\u00c1')};
        byte[] value = "\n  0064\"*NULL\"0000RC0FFDL1234#080027E62A64[70D7DC480FA7DC0DE428CE02447BA2E2981A161EA14CB780D2CD917208059E11\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];
        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptStore.parameters.put(
            "080027E62A64",
            Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build()
        );

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(13)
            .withMinuteOfHour(43)
            .withSecondOfMinute(9)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertTrue(message.isValid());
        Assertions.assertTrue(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.NULL, new String(crc), 100, 0, "1234", "080027E62A64");
    }

    @Test
    public void parseEventStandard1() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u007c'), UnsignedBytes.checkedCast('\u0037')};
        byte[] value = "\n  006b\"SIA-DCS\"0000RC0FFDL1234#080027E62A64[#080027E62A64|NHB0003][X115000][X115000][X115000]_13:53:11,06-22-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(13)
            .withMinuteOfHour(53)
            .withSecondOfMinute(11)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertEquals("SIA-DCS", message.getId());
        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.EVENT, new String(crc), 107, 0, "1234", "080027E62A64");
        Assertions.assertEquals("NHB0003", ((DataMessage) message).getData());
        Assertions.assertEquals(3, ((DataMessage) message).getAdditional().size());
        AdditionalData[] values = {new AdditionalData(
            AdditionalData.AdditionalDataType.LONGITUDE,
            "115000"
        ), new AdditionalData(AdditionalData.AdditionalDataType.LONGITUDE, "115000"), new AdditionalData(
            AdditionalData.AdditionalDataType.LONGITUDE,
            "115000"
        ),};
        int index = 0;
        for (AdditionalData additional : ((DataMessage) message).getAdditional()) {
            Assertions.assertEquals(values[index].getContent(), additional.getContent());
            Assertions.assertEquals(values[index].getType(), additional.getType());
            ++index;
        }
    }

    @Test
    public void parseEventStandard2() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u009b'), UnsignedBytes.checkedCast('\u006d')};
        byte[] value = "\n  0059\"SIA-DCS\"0000RC0FFDL1234#080027E62A64[#080027E62A64|NHB0003][E115000]_14:25:17,06-22-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(14)
            .withMinuteOfHour(25)
            .withSecondOfMinute(17)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertEquals("SIA-DCS", message.getId());
        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.EVENT, new String(crc), 89, 0, "1234", "080027E62A64");
        Assertions.assertEquals("NHB0003", ((DataMessage) message).getData());
        Assertions.assertEquals(1, ((DataMessage) message).getAdditional().size());
        AdditionalData[] values = {new AdditionalData(AdditionalData.AdditionalDataType.UNKNOWN, "E115000"),};
        int index = 0;
        for (AdditionalData additional : ((DataMessage) message).getAdditional()) {
            Assertions.assertEquals(values[index].getContent(), additional.getContent());
            Assertions.assertEquals(values[index].getType(), additional.getType());
            ++index;
        }
    }

    @Test
    public void parseEventStandard3() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u007e'), UnsignedBytes.checkedCast('\u00a8')};
        byte[] value = "\n  0059\"SIA-DCS\"0000Rc0ffdL1234#080027E62A64[#080027E62A64|NHB0003][E115000]_09:00:13,06-23-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(9)
            .withMinuteOfHour(0)
            .withSecondOfMinute(13)
            .withMillisOfSecond(0)
            .withDayOfMonth(23)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertEquals("SIA-DCS", message.getId());
        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("c0ffd", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.EVENT, new String(crc), 89, 0, "1234", "080027E62A64");
        Assertions.assertEquals("NHB0003", ((DataMessage) message).getData());
        Assertions.assertEquals(1, ((DataMessage) message).getAdditional().size());
        AdditionalData[] values = {new AdditionalData(AdditionalData.AdditionalDataType.UNKNOWN, "E115000"),};
        int index = 0;
        for (AdditionalData additional : ((DataMessage) message).getAdditional()) {
            Assertions.assertEquals(values[index].getContent(), additional.getContent());
            Assertions.assertEquals(values[index].getType(), additional.getType());
            ++index;
        }
    }

    @Test
    public void parseEventStandard4() throws InvalidMessageException {
        byte[] crc = {UnsignedBytes.checkedCast('\u0095'), UnsignedBytes.checkedCast('\u003a')};
        byte[] value = "\n  004c\"SIA-DCS\"0001L0#7878[#7878|Nri01^TOTAL^/LX901^centrale d'alarme JA-106K(R)^]\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        Assertions.assertEquals("SIA-DCS", message.getId());
        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertFalse(message.getReceiverNumber().isPresent());
        Assertions.assertFalse(message.getTimestamp().isPresent());
        validate(message, MessageType.EVENT, new String(crc), 76, 1, "0", "7878");
        Assertions.assertEquals("Nri01^TOTAL^/LX901^centrale d'alarme JA-106K(R)^", ((DataMessage) message).getData());
    }

    @Test
    public void parseEventStandardCiphered1() throws InvalidMessageException, InvalidCipheringException {

        byte[] crc = {UnsignedBytes.checkedCast('\u00e8'), UnsignedBytes.checkedCast('\u0088')};
        byte[] value = "\n  00a7\"*SIA-DCS\"0000RC0FFDL1234#080027E62A64[A054F111518361720BAB126501C6D994CD9151D2C84881C0FEA933A0790F49A9236EBD45F2AED32FAA252EB1AA18FC3CDEDE2452F33185E5931D31308D9B9882\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptStore.parameters.put(
            "080027E62A64",
            Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build()
        );
        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(12)
            .withSecondOfMinute(40)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        byte[] padding = {(byte) 0x9a, 0x10, (byte) 0xd2, (byte) 0x86, (byte) 0xc0, 0x38, 0x46, (byte) 0x89, 0x52, (byte) 0xf7, (byte) 0xac, 0x09};
        Assertions.assertTrue(message.getPadding().isPresent());
        Assertions.assertArrayEquals(padding, message.getPadding().get());

        Assertions.assertEquals("SIA-DCS", message.getId());
        Assertions.assertTrue(message.isValid());
        Assertions.assertTrue(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.EVENT, new String(crc), 167, 0, "1234", "080027E62A64");
        Assertions.assertEquals("NHB0003", ((DataMessage) message).getData());
        Assertions.assertEquals(1, ((DataMessage) message).getAdditional().size());
        AdditionalData[] values = {new AdditionalData(AdditionalData.AdditionalDataType.UNKNOWN, "E115000"),};
        int index = 0;
        for (AdditionalData additional : ((DataMessage) message).getAdditional()) {
            Assertions.assertEquals(values[index].getContent(), additional.getContent());
            Assertions.assertEquals(values[index].getType(), additional.getType());
            ++index;
        }
    }

    @Test
    public void parseEventStandardCiphered2() throws InvalidMessageException, InvalidCipheringException {

        byte[] crc = {UnsignedBytes.checkedCast('\u00a6'), UnsignedBytes.checkedCast('\n')};
        byte[] value = "\n  00a7\"*SIA-DCS\"0000Rc0ffdL1234#080027E62A64[1636385bf3ee2cf761f578f5b57eea422ca7601e0823c89d6f19feadf690104690e105e61d3989316bcb26df482a42dc3b4b3b0e9c40da4d3642e7d72cc5ce95\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptStore.parameters.put(
            "080027E62A64",
            Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build()
        );
        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(5)
            .withSecondOfMinute(2)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        byte[] padding = {0x21, (byte) 0xdb, 0x7e, 0x4d, (byte) 0xfb, 0x11, 0x61, 0x7a, 0x47, (byte) 0xdc, 0x6c, (byte) 0xe5};
        Assertions.assertTrue(message.getPadding().isPresent());
        Assertions.assertArrayEquals(padding, message.getPadding().get());

        Assertions.assertEquals("SIA-DCS", message.getId());
        Assertions.assertTrue(message.isValid());
        Assertions.assertTrue(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("c0ffd", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.EVENT, new String(crc), 167, 0, "1234", "080027E62A64");
        Assertions.assertEquals("NHB0003", ((DataMessage) message).getData());
        Assertions.assertEquals(1, ((DataMessage) message).getAdditional().size());
        AdditionalData[] values = {new AdditionalData(AdditionalData.AdditionalDataType.UNKNOWN, "E115000"),};
        int index = 0;
        for (AdditionalData additional : ((DataMessage) message).getAdditional()) {
            Assertions.assertEquals(values[index].getContent(), additional.getContent());
            Assertions.assertEquals(values[index].getType(), additional.getType());
            ++index;
        }
    }

    @Test
    public void parseRspStandard1() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u00fe'), UnsignedBytes.checkedCast('\u008f')};
        byte[] value = "\n  0055\"RSP\"0045RC0FFDL1234#080027E62A64[#080027E62A64|NHB0003][X115000]_15:15:25,06-22-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(15)
            .withSecondOfMinute(25)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertFalse(message.getPadding().isPresent());

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.RSP, new String(crc), 85, 45, "1234", "080027E62A64");
        Assertions.assertEquals("NHB0003", ((DataMessage) message).getData());
        Assertions.assertEquals(1, ((DataMessage) message).getAdditional().size());
        AdditionalData[] values = {new AdditionalData(AdditionalData.AdditionalDataType.LONGITUDE, "115000"),};
        int index = 0;
        for (AdditionalData additional : ((DataMessage) message).getAdditional()) {
            Assertions.assertEquals(values[index].getContent(), additional.getContent());
            Assertions.assertEquals(values[index].getType(), additional.getType());
            ++index;
        }
    }

    @Test
    public void parseRspStandard2() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u0080'), UnsignedBytes.checkedCast('\u00d7')};
        byte[] value = "\n  0055\"RSP\"0045Rc0ffdL1234#080027E62A64[#080027E62A64|NHB0003][X115000]_15:20:06,06-22-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(20)
            .withSecondOfMinute(6)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertFalse(message.getPadding().isPresent());

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("c0ffd", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.RSP, new String(crc), 85, 45, "1234", "080027E62A64");
        Assertions.assertEquals("NHB0003", ((DataMessage) message).getData());
        Assertions.assertEquals(1, ((DataMessage) message).getAdditional().size());
        AdditionalData[] values = {new AdditionalData(AdditionalData.AdditionalDataType.LONGITUDE, "115000"),};
        int index = 0;
        for (AdditionalData additional : ((DataMessage) message).getAdditional()) {
            Assertions.assertEquals(values[index].getContent(), additional.getContent());
            Assertions.assertEquals(values[index].getType(), additional.getType());
            ++index;
        }
    }

    @Test
    public void parseRspStandardCiphered1() throws InvalidMessageException, InvalidCipheringException {

        byte[] crc = {UnsignedBytes.checkedCast('\u00bf'), UnsignedBytes.checkedCast('\u006b')};
        byte[] value = "\n  00a3\"*RSP\"0045Rc0ffdL1234#080027E62A64[114be0aee403a1137690aea179b6c2d99f458d39cad3b929baaf8d30527e064f33dd518153b24d8a101442f90076cf7eb2ef324b91c6f80ca0db2a98a79e46be\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptStore.parameters.put(
            "080027E62A64",
            Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build()
        );
        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(33)
            .withSecondOfMinute(28)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        byte[] padding = {(byte) 0xf9, 0x28, 0x20, 0x3d, 0x4d, 0x10, (byte) 0x92, 0x68, (byte) 0x8c, 0x37, 0x2e, (byte) 0xf3};
        Assertions.assertTrue(message.getPadding().isPresent());
        Assertions.assertArrayEquals(padding, message.getPadding().get());

        Assertions.assertTrue(message.isValid());
        Assertions.assertTrue(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("c0ffd", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.RSP, new String(crc), 163, 45, "1234", "080027E62A64");
        Assertions.assertEquals("NHB0003", ((DataMessage) message).getData());
        Assertions.assertEquals(1, ((DataMessage) message).getAdditional().size());
        AdditionalData[] values = {new AdditionalData(AdditionalData.AdditionalDataType.LONGITUDE, "115000"),};
        int index = 0;
        for (AdditionalData additional : ((DataMessage) message).getAdditional()) {
            Assertions.assertEquals(values[index].getContent(), additional.getContent());
            Assertions.assertEquals(values[index].getType(), additional.getType());
            ++index;
        }
    }

    @Test
    public void parseRspStandardCiphered2() throws InvalidMessageException, InvalidCipheringException {

        byte[] crc = {UnsignedBytes.checkedCast('\u0031'), UnsignedBytes.checkedCast('\u00df')};
        byte[] value = "\n  00a3\"*RSP\"0045RC0FFDL1234#080027E62A64[19FCB2E3728C1FC31FEC939DC94ED87E6E1A812499644D92F46DB3D17C73A9305CA2C859F0AC37EF4C3DD3EB3930D6F7E6A2F6F3E016DEA5EF4766F079CA014D\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptStore.parameters.put(
            "080027E62A64",
            Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build()
        );
        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(35)
            .withSecondOfMinute(51)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        byte[] padding = {0x70, (byte) 0xc3, 0x34, 0x4f, (byte) 0xd2, 0x75, (byte) 0x94, (byte) 0xc9, 0x2e, (byte) 0xa8, 0x18, 0x11};
        Assertions.assertTrue(message.getPadding().isPresent());
        Assertions.assertArrayEquals(padding, message.getPadding().get());

        Assertions.assertTrue(message.isValid());
        Assertions.assertTrue(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.RSP, new String(crc), 163, 45, "1234", "080027E62A64");
        Assertions.assertEquals("NHB0003", ((DataMessage) message).getData());
        Assertions.assertEquals(1, ((DataMessage) message).getAdditional().size());
        AdditionalData[] values = {new AdditionalData(AdditionalData.AdditionalDataType.LONGITUDE, "115000"),};
        int index = 0;
        for (AdditionalData additional : ((DataMessage) message).getAdditional()) {
            Assertions.assertEquals(values[index].getContent(), additional.getContent());
            Assertions.assertEquals(values[index].getType(), additional.getType());
            ++index;
        }
    }

    @Test
    public void parseDuhStandardNoTimestamp1() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u001f'), UnsignedBytes.checkedCast('\u0020')};
        byte[] value = "\n  0023\"DUH\"0045Rc0ffdL1234#080027E62A64[]\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        Assertions.assertFalse(message.getPadding().isPresent());

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("c0ffd", message.getReceiverNumber().get());
        Assertions.assertFalse(message.getTimestamp().isPresent());
        validate(message, MessageType.DUH, new String(crc), 35, 45, "1234", "080027E62A64");

    }

    @Test
    public void parseDuhStandardNoTimestamp2() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u0014'), UnsignedBytes.checkedCast('\u0014')};
        byte[] value = "\n  0023\"DUH\"0045RC0FFDL1234#080027E62A64[]\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        Assertions.assertFalse(message.getPadding().isPresent());

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertFalse(message.getTimestamp().isPresent());
        validate(message, MessageType.DUH, new String(crc), 35, 45, "1234", "080027E62A64");

    }

    @Test
    public void parseDuhStandard1() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u0034'), UnsignedBytes.checkedCast('\u008f')};
        byte[] value = "\n  0037\"DUH\"0045Rc0ffdL1234#080027E62A64[]_15:59:33,06-22-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters.setHexToUpper(false), sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(59)
            .withSecondOfMinute(33)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertFalse(message.getPadding().isPresent());

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("c0ffd", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.DUH, new String(crc), 55, 45, "1234", "080027E62A64");

    }

    @Test
    public void parseDuhStandard2() throws InvalidMessageException {

        byte[] crc = {UnsignedBytes.checkedCast('\u0068'), UnsignedBytes.checkedCast('\u002e')};
        byte[] value = "\n  0037\"DUH\"0045RC0FFDL1234#080027E62A64[]_15:56:54,06-22-2015\r".getBytes();
        value[1] = crc[0];
        value[2] = crc[1];

        Dc09Handler handler = new Dc09Handler();
        Message message = handler.read(value, globalParameters, sptStore);

        DateTime expected = new DateTime().withZone(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(56)
            .withSecondOfMinute(54)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Assertions.assertFalse(message.getPadding().isPresent());

        Assertions.assertTrue(message.isValid());
        Assertions.assertFalse(message.isCiphered());
        Assertions.assertTrue(message.getReceiverNumber().isPresent());
        Assertions.assertEquals("C0FFD", message.getReceiverNumber().get());
        Assertions.assertEquals(expected, message.getTimestamp().get());
        validate(message, MessageType.DUH, new String(crc), 55, 45, "1234", "080027E62A64");

    }

}
