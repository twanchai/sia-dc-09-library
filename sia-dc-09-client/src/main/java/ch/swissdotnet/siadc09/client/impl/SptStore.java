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
package ch.swissdotnet.siadc09.client.impl;

import ch.swissdotnet.siadc09.Dc09SptStore;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class SptStore implements Dc09SptStore {

    private final Map<String, Dc09Spt> store = Maps.newConcurrentMap();

    public SptStore() {
    }

    public void addSpt(final Dc09Spt spt) {
        store.put(spt.getAccountNumber().toUpperCase(), spt);
    }

    public void removeSpt(final String accountNumber) {
        store.remove(accountNumber.toUpperCase());
    }

    public void removeSptById(final String id) {
        Stream<Dc09Spt> byId = store.values().stream().filter(new SptIdPredicate(id));
        for (Dc09Spt spt : byId.toList()) {
            store.remove(spt.getAccountNumber().toUpperCase());
        }
    }

    public Optional<Dc09Spt> retrieve(final String accountNumber) {
        String upperAccountNumber = accountNumber.toUpperCase();
        return Optional.ofNullable(store.get(upperAccountNumber));
    }

    @Override
    public Optional<Dc09Spt> retrieve(final Message message) {
        return retrieve(message.getAccountNumber());
    }

    public Optional<Dc09Spt> retrieveById(final String id) {
        return store.values().stream().filter(new SptIdPredicate(id)).findFirst();
    }

    @Override
    public Optional<Dc09Spt> findSpt(final Optional<String> optReceiverNumber,
                                     final String accountPrefix,
                                     final String accountNumber) {

        return retrieve(accountNumber);
    }


    private static class SptIdPredicate implements Predicate<Dc09Spt> {

        private final String id;

        public SptIdPredicate(final String id) {this.id = id;}

        @Override
        public boolean test(final Dc09Spt input) {
            return input != null
                && Objects.equals(input.getId(), id);
        }
    }
}
