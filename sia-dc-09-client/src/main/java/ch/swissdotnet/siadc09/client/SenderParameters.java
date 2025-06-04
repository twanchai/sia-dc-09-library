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
package ch.swissdotnet.siadc09.client;

import java.util.Objects;


public final class SenderParameters {

    private boolean overrideAccountNumber = true;
    private boolean overrideAccountDataNumber = true;
    private boolean overrideAccountPrefix = true;
    private boolean overrideReceiverNumber = true;
    private boolean overrideTimestamp = true;
    private boolean overrideSequence = true;

    public boolean isOverrideAccountNumber() {
        return overrideAccountNumber;
    }

    public SenderParameters setOverrideAccountNumber(final boolean overrideAccountNumber) {
        this.overrideAccountNumber = overrideAccountNumber;
        return this;
    }

    public boolean isOverrideAccountDataNumber() {
        return overrideAccountDataNumber;
    }

    public SenderParameters setOverrideAccountDataNumber(final boolean overrideAccountDataNumber) {
        this.overrideAccountDataNumber = overrideAccountDataNumber;
        return this;
    }

    public boolean isOverrideAccountPrefix() {
        return overrideAccountPrefix;
    }

    public SenderParameters setOverrideAccountPrefix(final boolean overrideAccountPrefix) {
        this.overrideAccountPrefix = overrideAccountPrefix;
        return this;
    }

    public boolean isOverrideReceiverNumber() {
        return overrideReceiverNumber;
    }

    public SenderParameters setOverrideReceiverNumber(final boolean overrideReceiverNumber) {
        this.overrideReceiverNumber = overrideReceiverNumber;
        return this;
    }

    public boolean isOverrideTimestamp() {
        return overrideTimestamp;
    }

    public SenderParameters setOverrideTimestamp(final boolean overrideTimestamp) {
        this.overrideTimestamp = overrideTimestamp;
        return this;
    }

    public boolean isOverrideSequence() {
        return overrideSequence;
    }

    public SenderParameters setOverrideSequence(final boolean overrideSequence) {
        this.overrideSequence = overrideSequence;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof final SenderParameters that)) return false;
        return overrideAccountNumber == that.overrideAccountNumber &&
            overrideAccountDataNumber == that.overrideAccountDataNumber &&
            overrideAccountPrefix == that.overrideAccountPrefix &&
            overrideReceiverNumber == that.overrideReceiverNumber &&
            overrideTimestamp == that.overrideTimestamp &&
            overrideSequence == that.overrideSequence;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            overrideAccountNumber,
            overrideAccountDataNumber,
            overrideAccountPrefix,
            overrideReceiverNumber,
            overrideTimestamp,
            overrideSequence
        );
    }
}
