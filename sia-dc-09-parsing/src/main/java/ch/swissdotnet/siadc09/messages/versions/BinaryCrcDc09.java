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
package ch.swissdotnet.siadc09.messages.versions;

import ch.swissdotnet.siadc09.Dc09Utils;
import com.google.common.io.BaseEncoding;

import static ch.swissdotnet.siadc09.Dc09Utils.BASE_16;
import static ch.swissdotnet.siadc09.Dc09Utils.BASE_16_LOWER;
import static ch.swissdotnet.siadc09.messages.crc.Crc16.calculateCrc;

/**
 * The {@code BinaryCrcDc09} class re-defines binary CRC message sizes and positions specifics.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class BinaryCrcDc09 extends StandardDc09 {


    @Override
    public int crcSize() {
        return 2;
    }

    @Override
    public byte[] crc(final byte[] middle, final boolean hexToUpper) {
        int crcValue = calculateCrc(middle);
        String hex = Dc09Utils.intToHexString(crcValue);
        BaseEncoding base16 = BASE_16;
        if (hexToUpper) {
            hex = hex.toUpperCase();
        } else {
            base16 = BASE_16_LOWER;
        }
        return base16.decode(hex);
    }

}
