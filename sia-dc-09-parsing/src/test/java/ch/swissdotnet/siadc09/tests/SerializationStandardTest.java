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
import ch.swissdotnet.siadc09.Dc09Writer;
import ch.swissdotnet.siadc09.exceptions.InvalidCipheringException;
import ch.swissdotnet.siadc09.exceptions.InvalidMessageException;
import ch.swissdotnet.siadc09.messages.AdditionalData;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.messages.encryption.AesCbcCipherAlgorithm;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.parameters.Dc09SptParameters;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


public class SerializationStandardTest {

    protected Dc09GlobalParameters globalParameters = new Dc09GlobalParameters();
    protected Dc09SptParameters sptParameters = new Dc09SptParameters();

    @Test
    public void serializeAckCorrectlyHexUpper() throws InvalidMessageException {
        String value = "\n2729002e\"ACK\"0001L0#080027E62A64[]_16:07:07,04-14-2015\r";

        DateTime expected = new DateTime()
            .withHourOfDay(16)
            .withMinuteOfHour(7)
            .withSecondOfMinute(7)
            .withMillisOfSecond(0)
            .withDayOfMonth(14)
            .withMonthOfYear(4)
            .withYear(2015);

        Dc09Handler handler = new Dc09Handler();
        Message message = Message.newAckBuilder()
            .sequence(1)
            .accountNumber("080027E62A64")
            .timestamp(expected)
            .build();
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeAckCorrectlyHexLower() throws InvalidMessageException {
        String value = "\n01fc0037\"ACK\"0000Rc0ffdL1234#080027E62A64[]_12:10:40,06-22-2015\r";

        DateTime expected = new DateTime()
            .withHourOfDay(12)
            .withMinuteOfHour(10)
            .withSecondOfMinute(40)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Dc09Handler handler = new Dc09Handler();
        Message message = Message.newAckBuilder()
            .sequence(0)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027E62A64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters.setHexToUpper(false), sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeAckCorrectlyWithPrefixAndReceiverNumber() throws InvalidMessageException {
        String value = "\n416A0036\"ACK\"0001R5678L1234#080027E62A64[]_16:07:53,04-14-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(16)
            .withMinuteOfHour(7)
            .withSecondOfMinute(53)
            .withMillisOfSecond(0)
            .withDayOfMonth(14)
            .withMonthOfYear(4)
            .withYear(2015);

        Message message = Message.newAckBuilder()
            .sequence(1)
            .receiverNumber("5678")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeAckCorrectlyWithPrefixAndReceiverNumberCipheredWithExceptionWrongKey()
        throws InvalidMessageException, InvalidCipheringException {

        String value = "\nE34C0062\"*ACK\"0001R5678L1234#080027E62A64[14F088FF5DCB19D1908069507EAB97C7CDB20F1C6EFA550BB59864D54B2DFA1C\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCE");
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey));
        Dc09Handler handler = new Dc09Handler();

        DateTime expected = DateTime.now();
        Message message = Message.newAckBuilder()
            .ciphered(true)
            .sequence(1)
            .receiverNumber("5678")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertNotEquals(new String(serialized.message()), value);
    }

    @Test
    public void serializeAckCorrectlyWithPrefixAndReceiverNumberCiphered1()
        throws InvalidMessageException, InvalidCipheringException {

        String value = "\nE34C0062\"*ACK\"0001R5678L1234#080027E62A64[14F088FF5DCB19D1908069507EAB97C7CDB20F1C6EFA550BB59864D54B2DFA1C\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(
            new AesCbcCipherAlgorithm(
                new SpecificPaddingGenerator(
                    new byte[] {0x12, (byte) 0xf1, (byte) 0xc2, 0x2a, (byte) 0x88, (byte) 0xeb, 0x44, 0x56, 0x79, (byte) 0xda}
                ),
                hexKey
            )
        );
        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(16)
            .withMinuteOfHour(8)
            .withSecondOfMinute(12)
            .withMillisOfSecond(0)
            .withDayOfMonth(14)
            .withMonthOfYear(4)
            .withYear(2015);

        Message message = Message.newAckBuilder()
            .ciphered(true)
            .sequence(1)
            .receiverNumber("5678")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeAckCorrectlyWithPrefixAndReceiverNumberCiphered2()
        throws InvalidMessageException, InvalidCipheringException {

        String value = "\n02B70063\"*ACK\"0000RC0FFDL1234#00900B376AB6[5F1BEAF37775DEC5F901894BDD25D9565C122B2BBAAD72E84E23B8F75554EF8C\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("28376EF26E782E1A4A160E3276E5FB93");
        sptParameters = new Dc09SptParameters(
            new AesCbcCipherAlgorithm(
                new SpecificPaddingGenerator(
                    new byte[] {0x73, 0x0b, 0x1d, (byte) 0x81, (byte) 0xa5, 0x45, 0x3a, 0x52, 0x6f, 0x57}
                ),
                hexKey
            )
        );
        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(8)
            .withMinuteOfHour(6)
            .withSecondOfMinute(26)
            .withMillisOfSecond(0)
            .withDayOfMonth(27)
            .withMonthOfYear(4)
            .withYear(2015);

        Message message = Message.newAckBuilder()
            .ciphered(true)
            .sequence(0)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("00900B376AB6")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeNakCorrectly() throws InvalidMessageException {
        String value = "\n3C830025\"NAK\"0000R0L0A0[]_16:09:09,04-14-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(16)
            .withMinuteOfHour(9)
            .withSecondOfMinute(9)
            .withMillisOfSecond(0)
            .withDayOfMonth(14)
            .withMonthOfYear(4)
            .withYear(2015);

        Message message = Message.newNakBuilder()
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeNakCipheredCorrectly()
        throws InvalidMessageException, InvalidCipheringException {

        String value = "\n3C830025\"NAK\"0000R0L0A0[]_16:09:09,04-14-2015\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(
            new AesCbcCipherAlgorithm(
                new SpecificPaddingGenerator(
                    new byte[] {0x4c, (byte) 0xf8, 0x58, (byte) 0x8f, 0x49, 0x2e, (byte) 0xea, 0x78, (byte) 0xcf, (byte) 0xae, 0x00}
                ),
                hexKey
            )
        );

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(16)
            .withMinuteOfHour(9)
            .withSecondOfMinute(9)
            .withMillisOfSecond(0)
            .withDayOfMonth(14)
            .withMonthOfYear(4)
            .withYear(2015);

        Message message = Message.newNakBuilder()
            .timestamp(expected)
            .ciphered(true)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializePollingStandard1() throws InvalidMessageException {
        String value = "\nC26E0038\"NULL\"0000RC0FFDL1234#080027E62A64[]_13:40:27,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(13)
            .withMinuteOfHour(40)
            .withSecondOfMinute(27)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newPollingBuilder()
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void serializePollingStandard2() throws InvalidMessageException {
        String value = "\n8a590038\"NULL\"0000Rc0ffdL1234#080027E62A64[]_15:50:24,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(50)
            .withSecondOfMinute(24)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newPollingBuilder()
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027E62A64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters.setHexToUpper(false), sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializePollingStandard3() throws InvalidMessageException {
        String value = "\n533F0038\"NULL\"0000RC0FFDL1234#080027E62A64[]_15:52:00,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(52)
            .withSecondOfMinute(0)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newPollingBuilder()
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void serializePollingStandardNoTimestamp1() throws InvalidMessageException {
        String value = "\n525a0024\"NULL\"0000Rc0ffdL1234#080027E62A64[]\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = Message.newPollingBuilder()
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027E62A64")
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters.setHexToUpper(false), sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializePollingStandardNoTimestamp2() throws InvalidMessageException {
        String value = "\n596E0024\"NULL\"0000RC0FFDL1234#080027E62A64[]\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = Message.newPollingBuilder()
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializePollingCiphered1() throws InvalidMessageException, InvalidCipheringException {
        String value = "\nCAC10064\"*NULL\"0000RC0FFDL1234#080027E62A64[70D7DC480FA7DC0DE428CE02447BA2E2981A161EA14CB780D2CD917208059E11\r";
        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(
            new AesCbcCipherAlgorithm(
                new SpecificPaddingGenerator(
                    new byte[] {0x4c, (byte) 0xf8, 0x58, (byte) 0x8f, 0x49, 0x2e, (byte) 0xea, 0x78, (byte) 0xcf, (byte) 0xae}
                ),
                hexKey
            )
        );

        Dc09Handler handler = new Dc09Handler();
        DateTime expected = new DateTime()
            .withHourOfDay(13)
            .withMinuteOfHour(43)
            .withSecondOfMinute(9)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newPollingBuilder()
            .ciphered(true)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void serializePollingCiphered2() throws InvalidMessageException, InvalidCipheringException {
        String value = "\n6A1B0067\"*NULL\"0000RFEDCLBA98#0123456789ABCDEF[DB5596C05AEC0F9CA8A9647985418EE2A818ABFC6AC5444B9A154F75E4FAABEC\r";
        byte[] hexKey = Dc09Utils.hexStringToBytes("1234567890ABCDEF1234567890ABCDEF");
        sptParameters = new Dc09SptParameters(
            new AesCbcCipherAlgorithm(
                new SpecificPaddingGenerator(
                    new byte[] {0x50, (byte) 0xb4, 0x6b, (byte) 0xf3, (byte) 0x89, (byte) 0xb0, (byte) 0xef, (byte) 0x92, 0x6e, (byte) 0xd5}
                ),
                hexKey
            )
        );

        Dc09Handler handler = new Dc09Handler();
        DateTime expected = new DateTime()
            .withHourOfDay(14)
            .withMinuteOfHour(55)
            .withSecondOfMinute(44)
            .withMillisOfSecond(0)
            .withMonthOfYear(6)
            .withDayOfMonth(29)
            .withYear(2015);

        Message message = Message.newPollingBuilder()
            .ciphered(true)
            .receiverNumber("FEDC")
            .accountPrefix("BA98")
            .accountNumber("0123456789ABCDEF")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void serializeEventStandard1() throws InvalidMessageException {
        String value = "\n7C37006b\"SIA-DCS\"0000RC0FFDL1234#080027E62A64[#080027E62A64|NHB0003][X115000][X115000][X115000]_13:53:11,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(13)
            .withMinuteOfHour(53)
            .withSecondOfMinute(11)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newEventBuilder("SIA-DCS", "NHB0003")
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .accountNumberData("080027e62a64")
            .additionalData(
                Lists.newArrayList(
                    new AdditionalData(AdditionalData.AdditionalDataType.LONGITUDE, "115000"),
                    new AdditionalData(AdditionalData.AdditionalDataType.LONGITUDE, "115000"),
                    new AdditionalData(AdditionalData.AdditionalDataType.LONGITUDE, "115000")
                )
            )
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void serializeEventStandard2() throws InvalidMessageException {
        String value = "\n9B6D0059\"SIA-DCS\"0000RC0FFDL1234#080027E62A64[#080027E62A64|NHB0003][E115000]_14:25:17,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(14)
            .withMinuteOfHour(25)
            .withSecondOfMinute(17)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newEventBuilder("SIA-DCS", "NHB0003")
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .accountNumberData("080027e62a64")
            .additionalData(
                Lists.newArrayList(
                    new AdditionalData(AdditionalData.AdditionalDataType.UNKNOWN, "E115000")
                )
            )
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void serializeEventStandard3() throws InvalidMessageException {
        String value = "\n7ea80059\"SIA-DCS\"0000Rc0ffdL1234#080027E62A64[#080027E62A64|NHB0003][E115000]_09:00:13,06-23-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(9)
            .withMinuteOfHour(0)
            .withSecondOfMinute(13)
            .withMillisOfSecond(0)
            .withDayOfMonth(23)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newEventBuilder("SIA-DCS", "NHB0003")
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027E62A64")
            .accountNumberData("080027E62A64")
            .additionalData(
                Lists.newArrayList(
                    new AdditionalData(AdditionalData.AdditionalDataType.UNKNOWN, "E115000")
                )
            )
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters.setHexToUpper(false), sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeEventStandard4() throws InvalidMessageException {
        String value = "\n52B5004c\"SIA-DCS\"0001L0#7777[#7777|Nri01^TOTAL^/LX901^centrale d'alarme JA-106K(R)^]\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = Message.newEventBuilder("SIA-DCS", "Nri01^TOTAL^/LX901^centrale d'alarme JA-106K(R)^")
            .sequence(1)
            .accountNumber("7777")
            .accountNumberData("7777")
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeEventStandard5AdditionalDataCharset() throws InvalidMessageException {
        String value = "\n52060082\"SIA-DCS\"0000RC0FFDL1234#080027E62A64[#080027E62A64|NHB0003][ITEST àäâèéêïîöôüÀÄÂÈÉÊÎÖÛÜ]_09:00:13,06-23-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(9)
            .withMinuteOfHour(0)
            .withSecondOfMinute(13)
            .withMillisOfSecond(0)
            .withDayOfMonth(23)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newEventBuilder("SIA-DCS", "NHB0003")
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027E62A64")
            .accountNumberData("080027E62A64")
            .additionalData(
                Lists.newArrayList(
                    new AdditionalData(AdditionalData.AdditionalDataType.ALARM_TEXT, "TEST àäâèéêïîöôüÀÄÂÈÉÊÎÖÛÜ")
                )
            )
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);
        System.out.println(new String(serialized.message()));

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeEventStandardCiphered1() throws InvalidMessageException, InvalidCipheringException {
        String value = "\nE88800a7\"*SIA-DCS\"0000RC0FFDL1234#080027E62A64[A054F111518361720BAB126501C6D994CD9151D2C84881C0FEA933A0790F49A9236EBD45F2AED32FAA252EB1AA18FC3CDEDE2452F33185E5931D31308D9B9882\r";
        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(
            new AesCbcCipherAlgorithm(
                new SpecificPaddingGenerator(
                    new byte[] {(byte) 0x9a, 0x10, (byte) 0xd2, (byte) 0x86, (byte) 0xc0, 0x38, 0x46, (byte) 0x89, 0x52, (byte) 0xf7, (byte) 0xac, 0x09}
                ),
                hexKey
            )
        );
        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(12)
            .withSecondOfMinute(40)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newEventBuilder("SIA-DCS", "NHB0003")
            .ciphered(true)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .accountNumberData("080027e62a64")
            .additionalData(
                Lists.newArrayList(
                    new AdditionalData(AdditionalData.AdditionalDataType.UNKNOWN, "E115000")
                )
            )
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeEventStandardCiphered2() throws InvalidMessageException, InvalidCipheringException {
        String value = "\na60a00a7\"*SIA-DCS\"0000Rc0ffdL1234#080027E62A64[1636385bf3ee2cf761f578f5b57eea422ca7601e0823c89d6f19feadf690104690e105e61d3989316bcb26df482a42dc3b4b3b0e9c40da4d3642e7d72cc5ce95\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(
            new AesCbcCipherAlgorithm(
                new SpecificPaddingGenerator(
                    new byte[] {0x21, (byte) 0xdb, 0x7e, 0x4d, (byte) 0xfb, 0x11, 0x61, 0x7a, 0x47, (byte) 0xdc, 0x6c, (byte) 0xe5}
                ),
                hexKey
            )
        );
        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(5)
            .withSecondOfMinute(2)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newEventBuilder("SIA-DCS", "NHB0003")
            .ciphered(true)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027E62A64")
            .accountNumberData("080027E62A64")
            .additionalData(
                Lists.newArrayList(
                    new AdditionalData(AdditionalData.AdditionalDataType.UNKNOWN, "E115000")
                )
            )
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters.setHexToUpper(false), sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeEventStandardCipheredRandomPadding() throws InvalidMessageException, InvalidCipheringException {

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey));
        Dc09Handler handler = new Dc09Handler();

        Dc09TestSptStore sptStore = new Dc09TestSptStore();
        sptStore.parameters.put(
            "080027e62a64", Dc09Spt.newSptBuilder(
                "080027E62A64",
                new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))
            ).build()
        );

        LocalDateTime expected = new LocalDateTime(DateTimeZone.UTC)
            .withHourOfDay(15)
            .withMinuteOfHour(12)
            .withSecondOfMinute(40)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newEventBuilder("SIA-DCS", "NHB0003")
            .ciphered(true)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .accountNumberData("080027e62a64")
            .additionalData(
                Lists.newArrayList(
                    new AdditionalData(AdditionalData.AdditionalDataType.UNKNOWN, "E115000")
                )
            )
            .timestamp(expected.toDateTime(DateTimeZone.UTC))
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters.setHexToUpper(false), sptParameters);

        Message read = handler.read(serialized.message(), globalParameters, sptStore);
        Assertions.assertEquals(message.isCiphered(), read.isCiphered());
        Assertions.assertEquals(message.getReceiverNumber().get(), read.getReceiverNumber().get());
        Assertions.assertEquals(message.getTimestamp().get(), read.getTimestamp().get());
        Assertions.assertEquals(message.getAccountPrefix(), read.getAccountPrefix());
        Assertions.assertEquals(message.getAccountNumberData(), read.getAccountNumberData());

    }

    @Test
    public void serializeRspStandard1() throws InvalidMessageException {
        String value = "\nE566004e\"RSP\"0045RC0FFDL1234#080027E62A64[#080027E62A64|NZZOR0003]_15:15:25,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(15)
            .withSecondOfMinute(25)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newRspBuilder("NZZOR0003")
            .sequence(45)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .accountNumberData("080027e62a64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        System.out.println(new String(value.getBytes()));
        System.out.println(new String(serialized.message()));
        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeRspStandard2() throws InvalidMessageException {
        String value = "\nD6C60059\"RSP\"0045RC0FFDL1234#080027E62A64[#080027E62A64|\"Swissdotnet\"RZZ22.5]_15:20:06,06-22-2015\r";
        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(20)
            .withSecondOfMinute(6)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newRspBuilder("\"Swissdotnet\"RZZ22.5")
            .sequence(45)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027E62A64")
            .accountNumberData("080027E62A64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        System.out.println(new String(value.getBytes()));
        System.out.println(new String(serialized.message()));

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeRspStandardCiphered1() throws InvalidMessageException, InvalidCipheringException {
        String value = "\nd6c50083\"*RSP\"0045Rc0ffdL1234#080027e62a64[8b22ea2febe71728403afe87417def856a5ca8aecd8f6dde0b42ba2ece561d574e1ffd09f2ac79876c8f2c7e2205641b\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(
            new AesCbcCipherAlgorithm(
                new SpecificPaddingGenerator(
                    new byte[] {(byte) 0xf9, 0x28, 0x20}
                ),
                hexKey
            )
        );
        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(33)
            .withSecondOfMinute(28)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newRspBuilder("NZZOR0003")
            .ciphered(true)
            .sequence(45)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .accountNumberData("080027e62a64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters.setHexToUpper(false), sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeRspStandardCiphered2() throws InvalidMessageException, InvalidCipheringException {
        String value = "\nDA8300a3\"*RSP\"0045RC0FFDL1234#080027E62A64[E55F0EB2AE870A9125D9AA2CDF55A38EE3F5F02C586F7E2BF46250D0E3BD097DF09893FE85570AE93DFE7677BEDEFB4B6E7E2705CE5DFEE692B478F3757DA5BF\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(
            new AesCbcCipherAlgorithm(
                new SpecificPaddingGenerator(
                    new byte[] {0x70, (byte) 0xc3, 0x34, 0x4f, (byte) 0xd2, 0x75, (byte) 0x94, (byte) 0xc9}
                ),
                hexKey
            )
        );
        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(35)
            .withSecondOfMinute(51)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newRspBuilder("\"Swissdotnet\"RZZ22.5")
            .ciphered(true)
            .sequence(45)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027E62A64")
            .accountNumberData("080027E62A64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    @Disabled // This is a test to see how RSP fare without data account number.
    public void serializeRspStandardCiphered3() throws InvalidMessageException, InvalidCipheringException {
        String value = "\n31DF00a3\"*RSP\"0045RC0FFDL1234#080027E62A64[19FCB2E3728C1FC31FEC939DC94ED87E6E1A812499644D92F46DB3D17C73A9305CA2C859F0AC37EF4C3DD3EB3930D6F7E6A2F6F3E016DEA5EF4766F079CA014D\r";

        // new SpecificPaddingGenerator(
        // new byte[] {0x70, (byte) 0xc3, 0x34, 0x4f, (byte) 0xd2, 0x75, (byte) 0x94, (byte) 0xc9, 0x2e, (byte) 0xa8, 0x18, 0x11}
        // ),
        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(
            new AesCbcCipherAlgorithm(

                hexKey
            )
        );
        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(35)
            .withSecondOfMinute(51)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newRspBuilder("NHB0003")
            .ciphered(true)
            .sequence(45)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);
        System.out.println(new String(serialized.clearData()));
        System.out.println(new String(serialized.message()));

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeDuhStandardNoTimestamp1() throws InvalidMessageException {
        String value = "\n1f200023\"DUH\"0045Rc0ffdL1234#080027E62A64[]\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = Message.newDuhBuilder()
            .sequence(45)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027E62A64")
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters.setHexToUpper(false), sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void serializeDuhStandardNoTimestamp2() throws InvalidMessageException {
        String value = "\n14140023\"DUH\"0045RC0FFDL1234#080027E62A64[]\r";

        Dc09Handler handler = new Dc09Handler();
        Message message = Message.newDuhBuilder()
            .sequence(45)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void serializeDuhStandard1() throws InvalidMessageException {
        String value = "\n348f0037\"DUH\"0045Rc0ffdL1234#080027E62A64[]_15:59:33,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(59)
            .withSecondOfMinute(33)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newDuhBuilder()
            .sequence(45)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027E62A64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters.setHexToUpper(false), sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void serializeDuhStandard2() throws InvalidMessageException {
        String value = "\n682E0037\"DUH\"0045RC0FFDL1234#080027E62A64[]_15:56:54,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        DateTime expected = new DateTime()
            .withHourOfDay(15)
            .withMinuteOfHour(56)
            .withSecondOfMinute(54)
            .withMillisOfSecond(0)
            .withDayOfMonth(22)
            .withMonthOfYear(6)
            .withYear(2015);

        Message message = Message.newDuhBuilder()
            .sequence(45)
            .receiverNumber("c0ffd")
            .accountPrefix("1234")
            .accountNumber("080027e62a64")
            .timestamp(expected)
            .build();

        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

}
