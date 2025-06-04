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
package ch.swissdotnet.siadc09.messages;

/**
 * The {@code Duh} class represents the message sent when RCT is unable to process an otherwise
 * correct message.
 * <p/>
 * {@code Duh} messages is <u>NEVER</u> ciphered.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class Duh extends Message {

    Duh(final Builder builder) {
        super(builder);
    }

    @Override
    public boolean isCiphered() {
        return false;
    }

    @Override
    public MessageType type() {
        return MessageType.DUH;
    }

}
