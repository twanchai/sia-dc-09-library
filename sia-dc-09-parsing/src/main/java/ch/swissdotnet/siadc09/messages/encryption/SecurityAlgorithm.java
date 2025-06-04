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
package ch.swissdotnet.siadc09.messages.encryption;

import static ch.swissdotnet.siadc09.messages.encryption.CipherAlgorithm.AES_ALGORITHM;

/**
 * The {@code SecurityAlgorithm} lists all security algorithm allowed for SIA DC-09.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public enum SecurityAlgorithm {
    /**
     * AES CBC 128 bits (key size 32 2-digit hexadecimal {@code String} -> 16 bytes).
     */
    AES_CBC_128(AES_ALGORITHM, 32),
    /**
     * AES CBC 192 bits (key size 48 2-digit hexadecimal {@code String} -> 24 bytes).
     */
    AES_CBC_192(AES_ALGORITHM, 48),
    /**
     * AES CBC 256 bits (key size 64 2-digit hexadecimal {@code String} -> 32 bytes).
     */
    AES_CBC_256(AES_ALGORITHM, 64),
    /**
     * Invalid security algorithm.
     */
    INVALID_ALGORITHM("INVALID", 0);

    private final String algorithm;
    private final int keySize;

    SecurityAlgorithm(final String algorithm, final int keySize) {
        this.algorithm = algorithm;
        this.keySize = keySize;
    }

    /**
     * Returns which {@code SecurityAlgorithm} should be used for given type and key size.
     *
     * @param type the security type
     * @param key  the 2-digit hexadecimal {@code String} used as key
     *
     * @return the {@code SecurityAlgorithm} for given type and key or {@code INVALID_ALGORITHM}
     */
    public static SecurityAlgorithm forKey(final String type, final String key) {
        int size = key.length();
        for (SecurityAlgorithm algorithm : values()) {
            if (algorithm.algorithm.equalsIgnoreCase(type)
                && algorithm.keySize == size) {
                return algorithm;
            }
        }
        return INVALID_ALGORITHM;
    }

    public static SecurityAlgorithm forKey(final String type, final byte[] key) {
        int size = key.length * 2;
        for (SecurityAlgorithm algorithm : values()) {
            if (algorithm.algorithm.equalsIgnoreCase(type)
                && algorithm.keySize == size) {
                return algorithm;
            }
        }
        return INVALID_ALGORITHM;
    }
}
