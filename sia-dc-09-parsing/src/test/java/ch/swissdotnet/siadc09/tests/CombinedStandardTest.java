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
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.messages.encryption.AesCbcCipherAlgorithm;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.parameters.Dc09SptParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class CombinedStandardTest {

    protected Dc09GlobalParameters globalParameters = new Dc09GlobalParameters();
    protected Dc09SptParameters sptParameters = new Dc09SptParameters();
    protected Dc09TestSptStore sptStore = new Dc09TestSptStore();

    @Test
    public void parseAckCorrectlyHexUpper() throws InvalidMessageException {
        String value = "\n2729002e\"ACK\"0001L0#080027E62A64[]_16:07:07,04-14-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseAckCorrectlyHexLower() throws InvalidMessageException {
        String value = "\n01fc0037\"ACK\"0000Rc0ffdL1234#080027E62A64[]_12:10:40,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseAckCorrectlyWithPrefixAndReceiverNumber() throws InvalidMessageException {
        String value = "\n416A0036\"ACK\"0001R5678L1234#080027E62A64[]_16:07:53,04-14-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseAckCorrectlyWithPrefixAndReceiverNumberCiphered1() throws InvalidMessageException, InvalidCipheringException {

        String value = "\nE34C0062\"*ACK\"0001R5678L1234#080027E62A64[14F088FF5DCB19D1908069507EAB97C7CDB20F1C6EFA550BB59864D54B2DFA1C\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey));
        sptStore.parameters.put("080027E62A64", Dc09Spt.newSptBuilder("080027E62A64", sptParameters).build());
        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(new SpecificPaddingGenerator(message.getPadding().get()), hexKey));
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseAckCorrectlyWithPrefixAndReceiverNumberCiphered2() throws InvalidMessageException, InvalidCipheringException {

        String value = "\n02B70063\"*ACK\"0000RC0FFDL1234#00900B376AB6[5F1BEAF37775DEC5F901894BDD25D9565C122B2BBAAD72E84E23B8F75554EF8C\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("28376EF26E782E1A4A160E3276E5FB93");
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey));
        sptStore.parameters.put("00900B376AB6", Dc09Spt.newSptBuilder("00900B376AB6", sptParameters).build());
        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(new SpecificPaddingGenerator(message.getPadding().get()), hexKey));
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseNakCorrectly() throws InvalidMessageException {

        String value = "\n3C830025\"NAK\"0000R0L0A0[]_16:09:09,04-14-2015\r";

        sptParameters = new Dc09SptParameters();
        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parsePollingStandard1() throws InvalidMessageException {
        String value = "\nC26E0038\"NULL\"0000RC0FFDL1234#080027E62A64[]_13:40:27,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parsePollingStandard2() throws InvalidMessageException {
        String value = "\n8a590038\"NULL\"0000Rc0ffdL1234#080027E62A64[]_15:50:24,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void parsePollingStandard3() throws InvalidMessageException {
        String value = "\n533F0038\"NULL\"0000RC0FFDL1234#080027E62A64[]_15:52:00,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void parsePollingStandardNoTimestamp1() throws InvalidMessageException {
        String value = "\n525a0024\"NULL\"0000Rc0ffdL1234#080027E62A64[]\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void parsePollingStandardNoTimestamp2() throws InvalidMessageException {
        String value = "\n596E0024\"NULL\"0000RC0FFDL1234#080027E62A64[]\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());

    }

    @Test
    public void parsePollingCiphered() throws InvalidMessageException, InvalidCipheringException {
        String value = "\nCAC10064\"*NULL\"0000RC0FFDL1234#080027E62A64[70D7DC480FA7DC0DE428CE02447BA2E2981A161EA14CB780D2CD917208059E11\r";
        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey));
        sptStore.parameters.put("080027E62A64", Dc09Spt.newSptBuilder("080027E62A64", sptParameters).build());

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(new SpecificPaddingGenerator(message.getPadding().get()), hexKey));
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseEventStandard1() throws InvalidMessageException {
        String value = "\n7C37006b\"SIA-DCS\"0000RC0FFDL1234#080027E62A64[#080027E62A64|NHB0003][X115000][X115000][X115000]_13:53:11,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseEventStandard2() throws InvalidMessageException {
        String value = "\n9B6D0059\"SIA-DCS\"0000RC0FFDL1234#080027E62A64[#080027E62A64|NHB0003][E115000]_14:25:17,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseEventStandard3() throws InvalidMessageException {
        String value = "\n7ea80059\"SIA-DCS\"0000Rc0ffdL1234#080027E62A64[#080027E62A64|NHB0003][E115000]_09:00:13,06-23-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseEventStandardCiphered1() throws InvalidMessageException, InvalidCipheringException {
        String value = "\nE88800a7\"*SIA-DCS\"0000RC0FFDL1234#080027E62A64[A054F111518361720BAB126501C6D994CD9151D2C84881C0FEA933A0790F49A9236EBD45F2AED32FAA252EB1AA18FC3CDEDE2452F33185E5931D31308D9B9882\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey));
        sptStore.parameters.put("080027E62A64", Dc09Spt.newSptBuilder("080027E62A64", sptParameters).build());
        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(new SpecificPaddingGenerator(message.getPadding().get()), hexKey));
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseEventStandardCiphered2() throws InvalidMessageException, InvalidCipheringException {
        String value = "\na60a00a7\"*SIA-DCS\"0000Rc0ffdL1234#080027E62A64[1636385bf3ee2cf761f578f5b57eea422ca7601e0823c89d6f19feadf690104690e105e61d3989316bcb26df482a42dc3b4b3b0e9c40da4d3642e7d72cc5ce95\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey));
        sptStore.parameters.put("080027E62A64", Dc09Spt.newSptBuilder("080027E62A64", sptParameters).build());
        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore);
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(new SpecificPaddingGenerator(message.getPadding().get()), hexKey));
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseRspStandard1() throws InvalidMessageException {
        String value = "\nFE8F0055\"RSP\"0045RC0FFDL1234#080027E62A64[#080027E62A64|NHB0003][X115000]_15:15:25,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseRspStandard2() throws InvalidMessageException {
        String value = "\n80d70055\"RSP\"0045Rc0ffdL1234#080027E62A64[#080027E62A64|NHB0003][X115000]_15:20:06,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseRspStandardCiphered1() throws InvalidMessageException, InvalidCipheringException {
        String value = "\nbf6b00a3\"*RSP\"0045Rc0ffdL1234#080027E62A64[114be0aee403a1137690aea179b6c2d99f458d39cad3b929baaf8d30527e064f33dd518153b24d8a101442f90076cf7eb2ef324b91c6f80ca0db2a98a79e46be\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey));
        sptStore.parameters.put("080027E62A64", Dc09Spt.newSptBuilder("080027E62A64", sptParameters).build());
        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore);
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(new SpecificPaddingGenerator(message.getPadding().get()), hexKey));
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseRspStandardCiphered2() throws InvalidMessageException, InvalidCipheringException {
        String value = "\n31DF00a3\"*RSP\"0045RC0FFDL1234#080027E62A64[19FCB2E3728C1FC31FEC939DC94ED87E6E1A812499644D92F46DB3D17C73A9305CA2C859F0AC37EF4C3DD3EB3930D6F7E6A2F6F3E016DEA5EF4766F079CA014D\r";

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey));
        sptStore.parameters.put("080027E62A64", Dc09Spt.newSptBuilder("080027E62A64", sptParameters).build());
        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        sptParameters = new Dc09SptParameters(new AesCbcCipherAlgorithm(new SpecificPaddingGenerator(message.getPadding().get()), hexKey));
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseDuhStandardNoTimestamp1() throws InvalidMessageException {
        String value = "\n1f200023\"DUH\"0045Rc0ffdL1234#080027E62A64[]\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseDuhStandardNoTimestamp2() throws InvalidMessageException {
        String value = "\n14140023\"DUH\"0045RC0FFDL1234#080027E62A64[]\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseDuhStandard1() throws InvalidMessageException {
        String value = "\n348f0037\"DUH\"0045Rc0ffdL1234#080027E62A64[]_15:59:33,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters.setHexToUpper(false), sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

    @Test
    public void parseDuhStandard2() throws InvalidMessageException {
        String value = "\n682E0037\"DUH\"0045RC0FFDL1234#080027E62A64[]_15:56:54,06-22-2015\r";

        Dc09Handler handler = new Dc09Handler();

        Message message = handler.read(value.getBytes(), globalParameters, sptStore);
        Dc09Writer.SerializedMessage serialized = handler.write(message, globalParameters, sptParameters);

        Assertions.assertArrayEquals(value.getBytes(), serialized.message());
    }

}
