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

import ch.swissdotnet.siadc09.messages.encryption.PaddingGenerator;
import org.junit.jupiter.api.Assertions;


public class SpecificPaddingGenerator implements PaddingGenerator {

    private final byte[] padding;

    public SpecificPaddingGenerator(final byte[] padding) {
        this.padding = padding;
    }

    @Override
    public byte[] padding(final int size) {
        Assertions.assertEquals(size, padding.length);
        return padding;
    }
}
