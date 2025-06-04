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

import java.util.Optional;

/**
 * The {@code InvalidMessageException} class may be thrown when reading and writing DC-09 message.
 * </p>
 * It may optionally contain a reference to the SPT concerned by the exception:
 * <ul>
 * <li>during read: once the message account number is defined,</li>
 * <li>during write: all the time.</li>
 * </ul>
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 * @see ch.swissdotnet.siadc09.Dc09Reader
 * @see ch.swissdotnet.siadc09.Dc09Writer
 */
public class InvalidMessageException extends Exception {

    // Optional reference to SPT through its account number.
    private final Optional<String> accountNumber;

    /**
     * Instantiate a new {@code InvalidMessageException}.
     */
    public InvalidMessageException() {
        super();
        accountNumber = Optional.empty();
    }

    /**
     * Instantiate a new {@code InvalidMessageException} with given message.
     *
     * @param message the message to wrap exception with
     */
    public InvalidMessageException(final String message) {
        super(message);
        accountNumber = Optional.empty();
    }

    /**
     * Instantiate a new {@code InvalidMessageException} with given exception (wrapped exception).
     *
     * @param e the exception to wrap exception with
     */
    public InvalidMessageException(final Exception e) {
        super(e);
        accountNumber = Optional.empty();
    }

    /**
     * Instantiate a new {@code InvalidMessageException} with given account number and message.
     *
     * @param accountNumber the SPT affected by the exception
     * @param message       the message to wrap exception with
     */
    public InvalidMessageException(final String accountNumber, final String message) {
        super(message);
        this.accountNumber = Optional.ofNullable(accountNumber);
    }

    /**
     * Instantiate a new {@code InvalidMessageException} with optionally given account number and message.
     *
     * @param accountNumber the optionally given SPT affected by the exception
     * @param message       the message to wrap exception with
     */
    public InvalidMessageException(final Optional<String> accountNumber, final String message) {
        super(message);
        this.accountNumber = accountNumber;
    }

    /**
     * Instantiate a new {@code InvalidMessageException} with given account number and exception.
     *
     * @param accountNumber the optionally given SPT affected by the exception
     * @param e             the exception to wrap exception with
     */
    public InvalidMessageException(final Optional<String> accountNumber, final InvalidCipheringException e) {
        super(e);
        this.accountNumber = accountNumber;
    }

    /**
     * @return the optionally present account number
     */
    public Optional<String> getAccountNumber() {
        return accountNumber;
    }

}
