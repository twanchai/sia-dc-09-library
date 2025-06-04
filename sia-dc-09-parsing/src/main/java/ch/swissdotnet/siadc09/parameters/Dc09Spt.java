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

import com.google.common.base.MoreObjects;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;

/**
 * The {@code Dc09Spt} class represents the SPT in the SIA DC-09 communication.
 * <p/>
 * It is defined by an account number, an account prefix and receiver number. Each SPT has its own
 * parameters (ciphering algorithm) and whether it is in diagnose or not. Optionally an ID can be
 * used to ease RCT retrieval by high level application.
 * <p/>
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class Dc09Spt {

    // The SPT ID (might be null).
    private final String id;
    // The SPT account number.
    private final String accountNumber;
    // The SPT account prefix.
    private final String accountPrefix;
    // The SPT receiver number.
    private final String receiverNumber;
    // The DC-09 parameters.
    private final Dc09SptParameters parameters;
    // The SPT optional mac address.
    private Optional<String> macAddress;
    // Whether to filter invalid timestamps or not.
    private boolean filterInvalidTimestamps;
    // Whether to disable timestamp check.
    private boolean disableTimestampChecks;
    // Whether to diagnose the SPT or not.
    private boolean diagnose;
    // Whether to use UTF-8 or default charset for DC-09 data.
    private boolean dataCharsetToUtf8;

    // Initializes a new Dc09Spt with given parameters.
    private Dc09Spt(final Builder builder) {
        this.id = builder.id;
        this.accountNumber = builder.accountNumber;
        this.accountPrefix = builder.accountPrefix;
        this.receiverNumber = builder.receiverNumber;
        this.macAddress = Optional.ofNullable(builder.macAddress);
        this.parameters = builder.sptParameters;
        this.filterInvalidTimestamps = builder.filterInvalidTimestamps;
        this.disableTimestampChecks = builder.disableTimestampChecks;
        this.diagnose = builder.diagnose;
        this.dataCharsetToUtf8 = builder.dataCharsetToUtf8;
    }

    /**
     * Creates a new {@code Builder} with given account number and SPT parameters.
     *
     * @param accountNumber the SPT account number
     * @param parameters    the SPT parameters
     *
     * @return the {@code Builder} instance
     */
    public static Builder newSptBuilder(final String accountNumber, final Dc09SptParameters parameters) {
        return new Builder()
            .accountNumber(accountNumber)
            .sptParameters(parameters);
    }

    /**
     * @return the SPT id (might be null)
     */
    public String getId() {
        return id;
    }

    /**
     * @return the SPT account number
     */
    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * @return the account prefix
     */
    public String getAccountPrefix() {
        return accountPrefix;
    }

    /**
     * @return the receiver number
     */
    public String getReceiverNumber() {
        return receiverNumber;
    }

    /**
     * @return the optionally present MAC address
     */
    public Optional<String> getMacAddress() {
        return macAddress;
    }

    /**
     * Sets (or resets) the SPT MAC address.
     *
     * @param macAddress the MAC address for the SPT
     *
     * @return the {@code Dc09Spt} instance
     */
    public Dc09Spt setMacAddress(final String macAddress) {
        this.macAddress = Optional.ofNullable(macAddress);
        return this;
    }

    /**
     * @return whether the SPT disable event timestamp checks or not
     */
    public boolean hasDisableTimestampCheck() {
        return disableTimestampChecks;
    }

    /**
     * Sets whether to disable passthrough for SPT events.
     *
     * @param disableTimestampChecks whether disable timestamp checks or not
     *
     * @return the {@code Dc09Spt} instance
     */
    public Dc09Spt setDisableTimestampChecks(final boolean disableTimestampChecks) {
        this.disableTimestampChecks = disableTimestampChecks;
        return this;
    }

    /**
     * @return whether the SPT disable event passthrough or not
     */
    public boolean hasFilterInvalidTimestamps() {
        return filterInvalidTimestamps;
    }

    /**
     * Sets whether to filter invalid timestamps or not.
     *
     * @param filterInvalidTimestamps whether filter invalid timestamps or not
     *
     * @return the {@code Dc09Spt} instance
     */
    public Dc09Spt setFilterInvalidTimestamps(final boolean filterInvalidTimestamps) {
        this.filterInvalidTimestamps = filterInvalidTimestamps;
        return this;
    }

    /**
     * @return the SPT parameters
     */
    public Dc09SptParameters getParameters() {
        return parameters;
    }

    /**
     * @return whether the SPT is in diagnose or not
     */
    public boolean isDiagnose() {
        return diagnose;
    }

    /**
     * Sets the SPT in diagnose or not.
     *
     * @param diagnose whether to put SPT in diagnose or not
     *
     * @return the {@code Dc09Spt} instance
     */
    public Dc09Spt setDiagnose(final boolean diagnose) {
        this.diagnose = diagnose;
        return this;
    }

    /**
     * @return whether to force UTF-8 for data charset
     */
    public boolean isDataCharsetToUtf8() {
        return dataCharsetToUtf8;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .omitNullValues()
            .add("id", id)
            .add("accountNumber", accountNumber)
            .add("accountPrefix", accountPrefix)
            .add("receiverNumber", receiverNumber)
            .add("parameters", parameters)
            .add("macAddress", macAddress)
            .add("filterInvalidTimestamps", filterInvalidTimestamps)
            .add("disableTimestampChecks", disableTimestampChecks)
            .add("diagnose", diagnose)
            .add("dataCharsetToUtf8", dataCharsetToUtf8)
            .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            accountNumber,
            accountPrefix,
            receiverNumber,
            parameters,
            macAddress,
            filterInvalidTimestamps,
            disableTimestampChecks,
            diagnose,
            dataCharsetToUtf8
        );
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Dc09Spt other = (Dc09Spt) obj;
        return Objects.equals(this.id, other.id)
            && Objects.equals(this.accountNumber, other.accountNumber)
            && Objects.equals(this.accountPrefix, other.accountPrefix)
            && Objects.equals(this.receiverNumber, other.receiverNumber)
            && Objects.equals(this.parameters, other.parameters)
            && Objects.equals(this.macAddress, other.macAddress)
            && Objects.equals(this.filterInvalidTimestamps, other.filterInvalidTimestamps)
            && Objects.equals(this.disableTimestampChecks, other.disableTimestampChecks)
            && Objects.equals(this.diagnose, other.diagnose)
            && Objects.equals(this.dataCharsetToUtf8, other.dataCharsetToUtf8);
    }

    /**
     * The {@code Builder} class allows to create {@code Dc09Spt}.
     */
    public static final class Builder {

        // SPT ID.
        private String id;
        // SPT account number.
        private String accountNumber;
        // Account prefix.
        private String accountPrefix;
        // Receiver number.
        private String receiverNumber;
        // SPT parameters.
        private Dc09SptParameters sptParameters;
        // Mac address.
        private String macAddress;
        // SPT diagnose.
        private boolean diagnose;
        // Filter invalid timestamps.
        private boolean disableTimestampChecks;
        // Disable event passthrough.
        private boolean filterInvalidTimestamps;
        // Whether to use UTF-8 or default charset for DC-09 additional data.
        private boolean dataCharsetToUtf8;

        // Initializes an empty Builder.
        private Builder() {}

        /**
         * Sets the SPT ID.
         *
         * @param id the ID to set
         *
         * @return the {@code Builder} instance
         */
        public Builder id(final String id) {
            this.id = checkNotNull(id);
            return this;
        }

        /**
         * Sets the SPT account number.
         *
         * @param accountNumber the account number to set
         *
         * @return the {@code Builder} instance
         */
        public Builder accountNumber(final String accountNumber) {
            this.accountNumber = checkNotNull(accountNumber);
            return this;
        }

        /**
         * Sets the account prefix.
         *
         * @param accountPrefix the account prefix to set
         *
         * @return the {@code Builder} instance
         */
        public Builder accountPrefix(final String accountPrefix) {
            this.accountPrefix = checkNotNull(accountPrefix);
            return this;
        }

        /**
         * Sets the receiver number.
         *
         * @param receiverNumber the receiver number to set
         *
         * @return the {@code Builder} instance
         */
        public Builder receiverNumber(final String receiverNumber) {
            this.receiverNumber = checkNotNull(receiverNumber);
            return this;
        }

        /**
         * Sets the MAC address.
         *
         * @param macAddress the MAC address to set
         *
         * @return the {@code Builder} instance
         */
        public Builder macAddress(final String macAddress) {
            this.macAddress = checkNotNull(macAddress);
            return this;
        }

        /**
         * Sets the SPT parameters.
         *
         * @param sptParameters the SPT parameters to set
         *
         * @return the {@code Builder} instance
         */
        public Builder sptParameters(final Dc09SptParameters sptParameters) {
            this.sptParameters = checkNotNull(sptParameters);
            return this;
        }

        /**
         * Sets whether to diagnose SPT or not.
         *
         * @param diagnose whether to diagnose SPT or not
         *
         * @return the {@code Builder} instance
         */
        public Builder diagnose(final boolean diagnose) {
            this.diagnose = diagnose;
            return this;
        }

        /**
         * Sets whether to filter invalid timestamps or not.
         *
         * @param filterInvalidTimestamps whether to filter invalid timestamps or not
         *
         * @return the {@code Builder} instance
         */
        public Builder filterInvalidTimestamps(final boolean filterInvalidTimestamps) {
            this.filterInvalidTimestamps = filterInvalidTimestamps;
            return this;
        }

        /**
         * Sets whether to disable timestamps checks or not
         *
         * @param disableTimestampChecks whether to disable timestamps checks or not
         *
         * @return the {@code Builder} instance
         */
        public Builder disableTimestampChecks(final boolean disableTimestampChecks) {
            this.disableTimestampChecks = disableTimestampChecks;
            return this;
        }

        /**
         * Sets whether to use UTF-8 as charset or not for additional data parsing
         *
         * @param dataCharsetToUtf8 whether to use UTF-8 or not
         *
         * @return the {@code Builder} instance
         */
        public Builder dataCharsetToUtf8(final boolean dataCharsetToUtf8) {
            this.dataCharsetToUtf8 = dataCharsetToUtf8;
            return this;
        }

        /**
         * @return a new {@code Dc09Spt} instance
         */
        public Dc09Spt build() {
            return new Dc09Spt(this);
        }

    }
}
