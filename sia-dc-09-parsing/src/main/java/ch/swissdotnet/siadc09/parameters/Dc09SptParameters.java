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
package ch.swissdotnet.siadc09.parameters;

import ch.swissdotnet.siadc09.messages.encryption.CipherAlgorithm;

import java.util.Optional;

/**
 * The {@code Dc09SptParameters} class holds SPT specific parameters.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public final class Dc09SptParameters {

    // Whether the SPT has a cipher algorithm.
    private final Optional<CipherAlgorithm> cipherAlgorithm;

    /**
     * Builds a blank {@code Dc09SptParameters}.
     */
    public Dc09SptParameters() {
        this.cipherAlgorithm = Optional.empty();
    }

    /**
     * Builds a new {@code Dc09SptParameters} with given cipher algorithm.
     *
     * @param cipherAlgorithm the cipher algorithm to use for given SPT
     */
    public Dc09SptParameters(final CipherAlgorithm cipherAlgorithm) {
        this.cipherAlgorithm = Optional.ofNullable(cipherAlgorithm);
    }

    /**
     * @return whether the SPT uses ciphered or not
     */
    public boolean usesCiphering() {
        return cipherAlgorithm.isPresent();
    }

    /**
     * @return the cipher algorithm after having check with {@link Dc09SptParameters#usesCiphering()}
     */
    public CipherAlgorithm cipherAlgorithm() {
        return cipherAlgorithm.get();
    }

}
