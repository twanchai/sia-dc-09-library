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
package ch.swissdotnet.siadc09.exceptions;

import ch.swissdotnet.siadc09.parameters.Dc09Spt;

import java.util.Optional;

/**
 * The {@code Dc09Exception} class is thrown when a server-side exception occurs.
 * <p/>
 * Used mostly to wrap an {@code Exception} and to add the concerned SPT or account number.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public final class Dc09Exception extends Exception {

    // The optionally present account number.
    private Optional<String> accountNumber = Optional.empty();
    // The optionally present SPT.
    private Optional<Dc09Spt> sptDc09 = Optional.empty();

    /**
     * Instantiates a blank {@code Dc09Exception}.
     */
    public Dc09Exception() {
    }

    /**
     * Instantiates a {@code Dc09Exception} with given message.
     *
     * @param message the message to wrap
     */
    public Dc09Exception(final String message) {
        super(message);
    }

    /**
     * Instantiates a {@code Dc09Exception} with given message and exception.
     *
     * @param message the message to wrap
     * @param cause   the exception to wrap
     */
    public Dc09Exception(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a {@code Dc09Exception} with given exception.
     *
     * @param cause the exception to wrap
     */
    public Dc09Exception(final Throwable cause) {
        super(cause);
    }

    /**
     * @return the optionally present account number
     */
    public Optional<String> getAccountNumber() {
        return accountNumber;
    }

    /**
     * Associate the exception with given account number.
     *
     * @param accountNumber the account number to associate exception with
     *
     * @return the {@code Dc09Exception} instance
     */
    public Dc09Exception setAccountNumber(final String accountNumber) {
        this.accountNumber = Optional.ofNullable(accountNumber);
        return this;
    }

    /**
     * @return the optionally present SPT
     */
    public Optional<Dc09Spt> getSptDc09() {
        return sptDc09;
    }

    /**
     * Associate the exception with given SPT.
     *
     * @param dc09Spt the SPT to associate exception with
     *
     * @return the {@code Dc09Exception} instance
     */
    public Dc09Exception setSptDc09(final Dc09Spt dc09Spt) {
        this.sptDc09 = Optional.ofNullable(dc09Spt);
        return this;
    }
}
