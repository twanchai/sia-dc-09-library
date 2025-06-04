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
 * The {@code Event} is the SPT message to indicate an event. It holds data and possibility extended data.
 * <p/>
 * The payload is not parsed and the ID indicate the payload type based on
 * "ANSI/SIA DC-09 2013, Annex H: DC-07 Protocol Identifier Tokens".
 * <p/>
 * {@code Event}s may be ciphered.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class Event extends DataMessage {

    Event(final Builder builder) {
        super(builder);
    }

    @Override
    public MessageType type() {
        return MessageType.EVENT;
    }

}
