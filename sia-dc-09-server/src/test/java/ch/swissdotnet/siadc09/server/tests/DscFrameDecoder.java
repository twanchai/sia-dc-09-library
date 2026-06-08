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
package ch.swissdotnet.siadc09.server.tests;

import ch.swissdotnet.siadc09.net.FrameUnwrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

/**
 * DSC TL-series proprietary binary frame unwrapper.
 *
 * <p>DSC panels (TL280, TL265, TL280RE, …) do not send raw SIA DC-09 frames; they wrap the
 * SIA payload in a proprietary binary envelope:</p>
 *
 * <pre>
 *  Byte  Field            Notes
 *  ----  ---------------  -----------------------------------------------
 *  00    Sync             0x5F  (DSC frame marker, not the SIA LF byte)
 *  01-02 Sequence         Device sub-ID / sequence number
 *  03-05 Account hash     3-byte value derived from the account number
 *  06-07 Flags            Option bits
 *  08-23 AES-128 block    Encrypted SIA DC-09 frame (single AES-CBC block,
 *                         zero IV — functionally equivalent to ECB for one block)
 *  24-27 CRC-32           CRC-32 of bytes 00–23 (little-endian)
 * </pre>
 *
 * <p>This class implements {@link FrameUnwrapper} so it can be plugged into the SIA DC-09
 * server pipeline transparently. When a frame does <em>not</em> start with {@code 0x5F} the
 * bytes are forwarded unchanged (standard SIA DC-09 processing path).</p>
 *
 * <h3>Configuration</h3>
 * <p>Construct with a list of AES-128 raw key bytes. All keys are tried in order; the first
 * one that produces a valid SIA DC-09 frame (starts with {@code 0x0A} LF byte) is used.
 * Pass {@code skipCrcValidation = true} during development/testing when the CRC algorithm
 * used by the target panel is not yet confirmed.</p>
 */
class DscFrameDecoder implements FrameUnwrapper {

    private static final Logger LOG = LoggerFactory.getLogger(DscFrameDecoder.class);

    /** DSC proprietary sync byte (first byte of every DSC frame). */
    static final int DSC_SYNC = 0x5F;

    /** Minimum frame size: 8 bytes header + 16 bytes AES block + 4 bytes CRC-32. */
    static final int FRAME_SIZE = 28;

    /** Byte offset of the 16-byte AES-128 encrypted SIA DC-09 payload. */
    static final int PAYLOAD_OFFSET = 8;

    /** Byte offset of the 4-byte CRC-32 checksum. */
    static final int CRC_OFFSET = 24;

    /** SIA DC-09 frame start character (LF = 0x0A). */
    static final int SIA_LF = 0x0A;

    /**
     * Default DSC AES-128 key: 16 zero bytes ({@code 00000000000000000000000000000000}).
     * Used when no key is explicitly configured.
     */
    static final byte[] DEFAULT_KEY = new byte[16];

    private final List<byte[]> keys;
    private final boolean skipCrcValidation;

    /**
     * Creates a {@code DscFrameDecoder} using the default all-zero AES key
     * ({@code 00000000000000000000000000000000}) with CRC validation enabled.
     */
    DscFrameDecoder() {
        this(List.of(DEFAULT_KEY), false);
    }

    /**
     * Creates a {@code DscFrameDecoder} with a single AES key and CRC validation enabled.
     *
     * @param key AES-128 raw key bytes (16 bytes); use {@link #DEFAULT_KEY} for the default
     */
    DscFrameDecoder(final byte[] key) {
        this(List.of(key), false);
    }

    /**
     * Creates a {@code DscFrameDecoder} with multiple AES keys and CRC validation enabled.
     *
     * @param keys list of AES-128 raw key bytes to try in order
     */
    DscFrameDecoder(final List<byte[]> keys) {
        this(keys, false);
    }

    /**
     * Creates a {@code DscFrameDecoder} with multiple AES keys.
     *
     * @param keys              list of AES-128 raw key bytes to try in order
     * @param skipCrcValidation when {@code true} CRC-32 check is skipped (useful when the
     *                          exact CRC byte order of the target panel is unknown)
     */
    DscFrameDecoder(final List<byte[]> keys, final boolean skipCrcValidation) {
        this.keys = List.copyOf(keys);
        this.skipCrcValidation = skipCrcValidation;
    }

    // ── FrameUnwrapper ────────────────────────────────────────────────────────

    @Override
    public byte[] tryUnwrap(final byte[] data) {
        if (!isDscFrame(data)) {
            return null; // not a DSC frame — pass through to standard SIA parser
        }

        LOG.debug("DSC binary frame detected ({} bytes), attempting unwrap.", data.length);

        if (!skipCrcValidation && !isCrcValid(data)) {
            LOG.warn("DSC frame CRC-32 mismatch — discarding frame.");
            return null;
        }

        byte[] encrypted = Arrays.copyOfRange(data, PAYLOAD_OFFSET, PAYLOAD_OFFSET + 16);

        for (int i = 0; i < keys.size(); i++) {
            byte[] decrypted = aesDecrypt(encrypted, keys.get(i));
            if (decrypted != null && (decrypted[0] & 0xFF) == SIA_LF) {
                LOG.debug("DSC frame unwrapped successfully using key index {}.", i);
                return decrypted;
            }
        }

        LOG.warn("DSC frame: none of the {} registered key(s) produced a valid SIA DC-09 frame.", keys.size());
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns {@code true} if the data looks like a DSC binary frame. */
    static boolean isDscFrame(final byte[] data) {
        return data != null && data.length >= FRAME_SIZE && (data[0] & 0xFF) == DSC_SYNC;
    }

    /**
     * Validates the CRC-32 in bytes 24–27 against bytes 0–23.
     * Tries little-endian first (x86 native), then big-endian.
     */
    static boolean isCrcValid(final byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data, 0, CRC_OFFSET);
        long computed = crc.getValue();

        // Little-endian (most likely for DSC/Windows-origin protocol)
        long leLong = ((data[24] & 0xFFL))
                    | ((data[25] & 0xFFL) << 8)
                    | ((data[26] & 0xFFL) << 16)
                    | ((data[27] & 0xFFL) << 24);
        if (computed == leLong) return true;

        // Big-endian fallback
        long beLong = ((data[24] & 0xFFL) << 24)
                    | ((data[25] & 0xFFL) << 16)
                    | ((data[26] & 0xFFL) << 8)
                    |  (data[27] & 0xFFL);
        return computed == beLong;
    }

    /**
     * Decrypts a single 16-byte AES-128-CBC block with a zero IV.
     * For a single block this is functionally equivalent to AES-128-ECB.
     *
     * @return decrypted bytes, or {@code null} on any error (wrong key length, etc.)
     */
    static byte[] aesDecrypt(final byte[] block, final byte[] key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
            return cipher.doFinal(block);
        } catch (Exception e) {
            LOG.trace("AES decrypt failed (likely wrong key): {}", e.getMessage());
            return null;
        }
    }
}
