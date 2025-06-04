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

import ch.swissdotnet.siadc09.Dc09SptStore;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;


class Dc09TestSptStore implements Dc09SptStore {

    public Map<String, Dc09Spt> parameters = Maps.newConcurrentMap();

    @Override
    public Optional<Dc09Spt> findSpt(final Optional<String> optReceiverNumber, final String accountPrefix, final String accountNumber) {
        return Optional.ofNullable(parameters.get(accountNumber));
    }

    @Override
    public Optional<Dc09Spt> retrieve(final Message message) {
        return parameters.values()
            .stream()
            .filter(Objects::nonNull)
            .filter(input -> Objects.equals(input.getAccountNumber(), message.getAccountNumber()))
            .findFirst();
    }
}
