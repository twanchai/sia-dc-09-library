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

import ch.swissdotnet.siadc09.exceptions.InvalidCipheringException;
import ch.swissdotnet.siadc09.messages.versions.Dc09Version;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * The {@code AesCipherAlgorithm} class implements an AES CBC ciphering/deciphering algorithm.
 * <p/>
 * It uses a random number generator which must generate valid SIA DC-09 padding value. The initialization
 * vector is zeroed as defined by "ANSI/SIA DC-09 2013" norm (p. 16).
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class AesCbcCipherAlgorithm implements CipherAlgorithm {

    // AES CBC No padding instance name on JVM.
    private static final String AES_CBC_NO_PADDING = "AES/CBC/NoPadding";
    // Padding generator.
    private final PaddingGenerator paddingGenerator;
    // Cipher engine.
    private final Cipher cipher;
    // Decipher engine.
    private final Cipher decipher;
    // Secret key used to cipher/decipher messages.
    private final SecretKey secretKey;
    // AES CBC algorithm parameters.
    private final AlgorithmParameterSpec paramSpec;

    /**
     * Instantiates a new {@code AesCipherAlgorithm} with given secret key.
     *
     * @param secretKey the secret key used to cipher/decipher
     *
     * @throws InvalidCipheringException when the secret key is invalid or
     *                                   the ciphering algorithm cannot be built with given parameters
     */
    public AesCbcCipherAlgorithm(final byte[] secretKey) throws InvalidCipheringException {
        this(new RandomPaddingGenerator(), secretKey);
    }

    /**
     * Instantiates a new {@code AesCipherAlgorithm} with given secret key.
     *
     * @param paddingGenerator the padding generator which must follow SIA DC-09 specification
     * @param key              the secret key used to cipher/decipher
     *
     * @throws InvalidCipheringException when the secret key is invalid or
     *                                   the ciphering algorithm cannot be built with given parameters
     */
    public AesCbcCipherAlgorithm(final PaddingGenerator paddingGenerator,
                                 final byte[] key) throws InvalidCipheringException {

        this.paddingGenerator = paddingGenerator;
        try {
            secretKey = new SecretKeySpec(key, AES_ALGORITHM);
        } catch (final Throwable t) {
            throw new InvalidCipheringException("Invalid cipher: Not able to build secret key");
        }
        SecurityAlgorithm securityAlgorithm = SecurityAlgorithm.forKey(AES_ALGORITHM, key);
        if (securityAlgorithm == SecurityAlgorithm.INVALID_ALGORITHM) {
            throw new InvalidCipheringException("Invalid cipher: Not able to build secret key");
        }

        // Initialization vector to 0.
        byte[] iv = {
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        };

        paramSpec = new IvParameterSpec(iv);
        try {
            cipher = Cipher.getInstance(AES_CBC_NO_PADDING);
            decipher = Cipher.getInstance(AES_CBC_NO_PADDING);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new InvalidCipheringException("Invalid cipher: Not able to create AES instance with provided key");
        }

    }

    @Override
    public byte[] cipher(final byte[] clear, final Dc09Version version) throws InvalidCipheringException {

        // Uses the SIA DC-09 alignment.
        int encryptedAlignment = version.encryptedAlignment();
        int paddingRequired = encryptedAlignment - (clear.length % encryptedAlignment);
        if (paddingRequired == 0) {
            paddingRequired = encryptedAlignment;
        }

        byte[] padding = paddingGenerator.padding(paddingRequired);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(padding);
            baos.write(clear);
            synchronized (cipher) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
                return cipher.doFinal(baos.toByteArray());
            }
        } catch (final IOException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new InvalidCipheringException("Invalid cipher: Unable to cipher incoming cleared data (" + e.getMessage() + ")");
        }

    }

    @Override
    public byte[] decipher(final byte[] cipherData) throws InvalidCipheringException {
        try {
            synchronized (decipher) {
                decipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
                return decipher.doFinal(cipherData);
            }
        } catch (final IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new InvalidCipheringException("Invalid cipher: Unable to decipher incoming data (" + e.getMessage() + ")");
        }
    }
}
