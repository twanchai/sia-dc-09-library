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

import ch.swissdotnet.siadc09.Dc09Utils;
import ch.swissdotnet.siadc09.exceptions.InvalidMessageException;
import ch.swissdotnet.siadc09.messages.encryption.RandomPaddingGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;


public class UtilsTest {

    public static final byte[] BYTES = new byte[] {0x10, 0x11, 0x12, 0x13, 0x14};

    @Test
    @Timeout (5)
    public void testRandomGenerator() {
        byte[] padding = new RandomPaddingGenerator().padding(1000000);
        for (byte value : padding) {
            if (RandomPaddingGenerator.PROHIBITED_CHARACTERS.contains(value)) {
                Assertions.fail("Should not have contained invalid value ([, ] or |)");
            }
        }
    }

    @Test
    public void testArrayCopy() {
        byte[] padding = new RandomPaddingGenerator().padding(1000);
        byte[] copied = Dc09Utils.copy(padding);
        Assertions.assertArrayEquals(padding, copied);
    }

    @Test
    public void testBytesToAsciiString1() {
        byte[] hex = {0x41, 0x42, 0x30, (byte) 0xf1, 0x01};
        Assertions.assertEquals("AB0..", Dc09Utils.bytesToAsciiString(hex));
    }

    @Test
    public void testBytesToAsciiString2() {
        byte[] hex = {};
        Assertions.assertEquals("", Dc09Utils.bytesToAsciiString(hex));
    }

    @Test
    public void testBytesToHexString1() {
        byte[] hex = {0x41, 0x42, 0x30, (byte) 0xf1, 0x01};
        Assertions.assertEquals("41 42 30 f1 01", Dc09Utils.bytesToHexString(hex));
    }

    @Test
    public void testBytesToHexString2() {
        byte[] hex = {};
        Assertions.assertEquals("", Dc09Utils.bytesToHexString(hex));
    }

    @Test
    public void testHexStringToBytes1() {
        String hexString = "AFFFF140414401";
        byte[] hex = {(byte) 0xaf, (byte) 0xff, (byte) 0xf1, 0x40, 0x41, 0x44, 0x01};
        Assertions.assertArrayEquals(hex, Dc09Utils.hexStringToBytes(hexString));
    }

    @Test
    public void testHexStringToBytes2() {
        String hexString = "";
        byte[] hex = {};
        Assertions.assertArrayEquals(hex, Dc09Utils.hexStringToBytes(hexString));
    }

    @Test
    public void testHexStringToBytes3() {
        String hexString = "F";
        byte[] hex = {};

        assertThrows(
            IllegalArgumentException.class, () -> Assertions.assertArrayEquals(hex, Dc09Utils.hexStringToBytes(hexString))
        );
    }

    @Test
    public void testHexStringToBytes4() {
        String hexString = "FZ";
        byte[] hex = {};
        assertThrows(
            IllegalArgumentException.class, () -> Assertions.assertArrayEquals(hex, Dc09Utils.hexStringToBytes(hexString))
        );
    }

    @Test
    public void testIsHexDigit1() {
        String hexString = "AFFFF140414401";
        Assertions.assertTrue(Dc09Utils.isHexDigit(hexString));
        Assertions.assertTrue(Dc09Utils.is2HexDigit(hexString));
    }

    @Test
    public void testIsHexDigit2() {
        String hexString = "";
        Assertions.assertTrue(Dc09Utils.isHexDigit(hexString));
        Assertions.assertTrue(Dc09Utils.is2HexDigit(hexString));
    }

    @Test
    public void testIsHexDigit3() {
        String hexString = "F";
        Assertions.assertTrue(Dc09Utils.isHexDigit(hexString));
        Assertions.assertFalse(Dc09Utils.is2HexDigit(hexString));
    }

    @Test
    public void testIsHexDigit4() {
        String hexString = "FZ";
        Assertions.assertFalse(Dc09Utils.isHexDigit(hexString));
        Assertions.assertFalse(Dc09Utils.is2HexDigit(hexString));
    }

    @Test
    public void testHexToInt1() throws InvalidMessageException {
        byte[] hex = {0, 0, (byte) 0x40, 0x41};
        Assertions.assertEquals(16449, Dc09Utils.hexBytesToInt(Dc09Utils.bytesToHexString(hex).replaceAll(" ", "").getBytes()));
    }

    @Test
    public void testHexToInt2() throws InvalidMessageException {
        byte[] hex = {0, 0, 0, 0};
        Assertions.assertEquals(0, Dc09Utils.hexBytesToInt(Dc09Utils.bytesToHexString(hex).replaceAll(" ", "").getBytes()));
    }

    @Test
    public void testHexToInt3() {
        byte[] hex = {0, 0, 0, 1, 0, 0, 0, 1};

        assertThrows(
            InvalidMessageException.class,
            () -> Assertions.assertEquals(0, Dc09Utils.hexBytesToInt(Dc09Utils.bytesToHexString(hex).replaceAll(" ", "").getBytes()))
        );
    }

    @Test
    public void testHexToInt4() {
        assertThrows(
            InvalidMessageException.class, () -> Assertions.assertEquals(0, Dc09Utils.hexBytesToInt("AZ".getBytes()))
        );
    }

    @Test
    public void testLongToHex1() {
        int value = 10;
        Assertions.assertEquals("000a", Dc09Utils.intToHexString(value));
    }

    @Test
    public void testLongToHex2() {
        int value = 1000;
        Assertions.assertEquals("03e8", Dc09Utils.intToHexString(value));
    }

    @Test
    public void testIncrementAttesting1() throws InvalidMessageException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[] {0x00});
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Dc09Utils.incrementAttestingValue(bais, (byte) 0, Optional.empty());
    }

    @Test
    public void testIncrementAttesting2() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[] {});
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        assertThrows(InvalidMessageException.class, () -> Dc09Utils.incrementAttestingValue(bais, (byte) 0, Optional.empty()));
    }

    @Test
    public void testIncrementAttesting3() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[] {0x10});
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertThrows(InvalidMessageException.class, () -> Dc09Utils.incrementAttestingValue(bais, (byte) 0, Optional.empty()));
    }

    @Test
    public void testReadByte1() throws InvalidMessageException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[] {0x10});
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        byte value = Dc09Utils.readByte(bais, Optional.empty());
        Assertions.assertEquals(0x10, value);
    }

    @Test
    public void testReadByte2() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[] {});
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertThrows(InvalidMessageException.class, () -> Dc09Utils.readByte(bais, Optional.empty()));
    }

    @Test
    public void testReadBytes1() throws InvalidMessageException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BYTES);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        byte[] value = Dc09Utils.readBytes(bais, 5, Optional.empty());
        Assertions.assertArrayEquals(BYTES, value);
    }

    @Test
    public void testReadBytes2() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BYTES);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertThrows(InvalidMessageException.class, () -> Dc09Utils.readBytes(bais, 6, Optional.empty()));
    }

    @Test
    public void testReadUntil1() throws InvalidMessageException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BYTES);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        byte[] value = Dc09Utils.readUntil(bais, (byte) 0x12, 6, Optional.empty());
        Assertions.assertArrayEquals(new byte[] {0x10, 0x11}, value);
    }

    @Test
    public void testReadUntil2() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BYTES);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertThrows(InvalidMessageException.class, () -> Dc09Utils.readUntil(bais, (byte) 0x16, 4, Optional.empty()));
    }

    @Test
    public void testReadUntil3() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BYTES);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertThrows(InvalidMessageException.class, () -> Dc09Utils.readUntil(bais, (byte) 0x16, 7, Optional.empty()));
    }

    @Test
    public void testIndexOf1() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BYTES);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        int indexOf = Dc09Utils.indexOf(bais, (byte) 0x16, 2);
        Assertions.assertEquals(-1, indexOf);
    }

    @Test
    public void testIndexOf2() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BYTES);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        int indexOf = Dc09Utils.indexOf(bais, (byte) 0x11, 5);
        Assertions.assertEquals(1, indexOf);
    }

    @Test
    public void testIndexOf3() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] array = new byte[] {};
        baos.write(array);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        int indexOf = Dc09Utils.indexOf(bais, (byte) 0x11, 5);
        Assertions.assertEquals(-1, indexOf);
    }

    @Test
    public void testPeek1() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] array = new byte[] {};
        baos.write(array);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        byte peek = Dc09Utils.peek(bais);
        Assertions.assertEquals(-1, peek);
    }

    @Test
    public void testPeek2() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BYTES);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        byte peek = Dc09Utils.peek(bais);
        Assertions.assertEquals((byte) 0x10, peek);
    }

    @Test
    public void testPeekBytes1() throws InvalidMessageException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(BYTES);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        byte[] peek = Dc09Utils.peek(bais, 10, Optional.empty());
        byte[] padded = new byte[10];
        System.arraycopy(BYTES, 0, padded, 0, BYTES.length);
        Assertions.assertArrayEquals(padded, peek);
    }

    @Test
    public void testPeekBytes2() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] array = new byte[] {};
        baos.write(array);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        byte peek = Dc09Utils.peek(bais);
        Assertions.assertEquals(-1, peek);
    }

}
