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
package ch.swissdotnet.siadc09;

import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;

import java.util.Optional;

/**
 * The {@code Dc09SptStore} interfaces defines how to lookup SPT based on their IDs or received message.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public interface Dc09SptStore {

    /**
     * Tries to find stored SPT for specified IDs.
     *
     * @param optReceiverNumber the receiver number associated with SPT
     * @param accountPrefix     the account prefix associated with SPT
     * @param accountNumber     the account number associated with SPT
     *
     * @return the possibly stored {@code Dc09Spt}
     */
    Optional<Dc09Spt> findSpt(final Optional<String> optReceiverNumber,
                              final String accountPrefix,
                              final String accountNumber);

    /**
     * Tries to find stored SPT with given message.
     *
     * @param message the message to find SPT with
     *
     * @return the possibly stored {@code Dc09Spt}
     */
    Optional<Dc09Spt> retrieve(final Message message);
}
