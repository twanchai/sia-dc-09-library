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
package ch.swissdotnet.siadc09.net;

/**
 * The {@code FrameUnwrapper} interface allows plugging a proprietary frame decoder into the
 * SIA DC-09 receive pipeline.
 * <p/>
 * Implementations receive the raw bytes from the transport layer and may unwrap them into
 * standard SIA DC-09 bytes. The unwrapper is invoked before the SIA DC-09 parser so the rest
 * of the pipeline sees a plain SIA DC-09 frame regardless of the outer framing.
 *
 * <p>Example use-case: DSC TL-series panels wrap the SIA DC-09 payload in a proprietary binary
 * envelope (sync byte {@code 0x5F}, sequence, account hash, flags, 16-byte AES-128 block,
 * CRC-32). A {@code FrameUnwrapper} implementation can strip that envelope and return the
 * inner SIA DC-09 bytes transparently.</p>
 */
public interface FrameUnwrapper {

    /**
     * Attempts to unwrap a proprietary frame into raw SIA DC-09 bytes.
     *
     * @param data the raw bytes received from the transport layer
     * @return the unwrapped SIA DC-09 bytes, or {@code null} if the frame is not handled by
     *         this unwrapper (in which case the original bytes are forwarded unchanged)
     */
    byte[] tryUnwrap(byte[] data);

}
