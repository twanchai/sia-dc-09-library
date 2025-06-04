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
package ch.swissdotnet.siadc09.server.listeners;

import ch.swissdotnet.siadc09.RemoteAddressResolver;
import ch.swissdotnet.siadc09.exceptions.Dc09Exception;
import ch.swissdotnet.siadc09.messages.Event;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.messages.MessageType;
import ch.swissdotnet.siadc09.messages.Polling;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.server.RctDc09;
import com.google.common.base.Throwables;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static ch.swissdotnet.siadc09.Dc09Utils.bytesToAsciiString;
import static org.joda.time.DateTimeZone.UTC;
import static org.joda.time.LocalDateTime.now;

/**
 * The {@code RctMessageFilter} class implements the basic SIA DC-09 RCT message handling.
 * <p/>
 * Depending on parameters, might allow or reject messages. The messages are dispatched to {@code RctListener}
 * upon acceptation. The response builder given already filled with incoming message value:
 * <ul>
 * <li>version</li>
 * <li>sequence number</li>
 * <li>account prefix</li>
 * <li>receiver number</li>
 * <li>account number</li>
 * <li>whether to cipher or not the response</li>
 * </ul>
 * <p/>
 * The filtering done by the {@link #onMessage(Message, DateTime, Dc09Spt, RctDc09, RemoteAddressResolver, ServerResponseListener)} method is:
 * <ul>
 * <li>whether the message is valid or not (CRC + length)</li>
 * <li>
 * when dealing with ciphered messages:
 * <ul>
 * <li>timestamp is present (might still accept message if disable timestamp checks is set)</li>
 * <li>timestamp is in range (might still accept message if disable timestamp checks is set)</li>
 * <li>if disable timestamp checks, and timestamp is invalid, NAK is sent either way but let the message through</li>
 * </ul>
 * </li>
 * <li>
 * when dealing with unencrypted messages:
 * <ul>
 * <li>whether we reject unencrypted messages or not</li>
 * <li>timestamp is present and valid (always accept even with invalid timestamp)</li>
 * </ul>
 * </li>
 * <li>the message is either a polling message or event, otherwise an error is raised</li>
 * </ul>
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 * @see Dc09GlobalParameters
 * @see RctListener
 */
public class RctMessageFilter implements ServerMessageListener {

    // SLF4J Logger
    private static final Logger LOG = LoggerFactory.getLogger(RctMessageFilter.class);
    private final Dc09GlobalParameters configuration;
    private final RctListener rctListener;

    /**
     * Initializes a new {@code RctMessageFilter} with given configuration and RCT listener.
     *
     * @param configuration the configuration to enforce
     * @param rctListener   the listener to call when accepting message
     */
    public RctMessageFilter(final Dc09GlobalParameters configuration,
                            final RctListener rctListener) {
        this.configuration = configuration;
        this.rctListener = rctListener;
    }

    /**
     * Checks whether the message timestamp is valid or not.
     * <p/>
     * When no timestamp, it is invalid. When it is not in range [reception - min, reception + max] it is invalid.
     *
     * @param message   the message received
     * @param reception when the message is received
     * @param max       the maximum value plus reception time to accept message
     * @param min       the minimum value minus reception time to accept message
     *
     * @return whether the timestamp is valid or not
     */
    public static boolean isTimestampValid(final Message message,
                                           final DateTime reception,
                                           final int max,
                                           final int min) {
        Optional<DateTime> optDateTime = message.getTimestamp();
        if (optDateTime.isEmpty()) {
            return false;
        }
        DateTime timestamp = optDateTime.get();
        boolean isBeforeReceivedPlusMax = reception.plusSeconds(max).isAfter(timestamp);
        boolean isBeforeReceivedMinusMin = reception.minusSeconds(min).isBefore(timestamp);
        return isBeforeReceivedPlusMax && isBeforeReceivedMinusMin;
    }

    // Direction pretty printer.
    private static String direction(final Direction direction) {return direction == Direction.INCOMING ? "received" : "sent";}

    // SPT pretty printer.
    private static String spt(final Optional<Dc09Spt> sptDc09) {
        return sptDc09.isPresent() ? sptDc09.get().getAccountNumber() : "unknown";
    }

    @Override
    public void onMessage(final Message message,
                          final DateTime reception,
                          final Dc09Spt spt,
                          final RctDc09 rct,
                          final RemoteAddressResolver address,
                          final ServerResponseListener listener) {

        String accountNumber = message.getAccountNumber();
        int maxDifferenceWithServer = configuration.getMaxDifferenceWithServer();
        int minDifferenceWithServer = configuration.getMinDifferenceWithServer();

        Message.Builder response = Message.newAckBuilder()
            .version(message.getVersion())
            .sequence(message.getSequence())
            .accountPrefix(message.getAccountPrefix())
            .receiverNumber(message.getReceiverNumber())
            .accountNumber(accountNumber)
            .accountNumberData(accountNumber)
            .ciphered(message.isCiphered());

        // When a message is invalid, no response given back.
        if (!message.isValid()) {
            rctListener.onError("Invalid message received. Discarding message.", address, message);
            listener.close();
            return;
        }

        // Stores current time as UTC.
        DateTime now = now(UTC).toDateTime(UTC);

        // When the message is ciphered, check whether the timestamp is valid or not.
        // To accept the message even when timestamp check fails, it seeks the nakIsAck flag.
        if (message.isCiphered()) {

            // No timestamp reception in ciphered mode. Check whether we acknowledge it or not.
            if (message.getTimestamp().isEmpty()) {
                rctListener.onError("No timestamp received. Discarding message.", address, message);
                listener.close();
                return;
            }

            // When a message is ciphered, the response must hold a timestamp.
            response.timestamp(now);

            boolean timestampValid = isTimestampValid(message, reception, maxDifferenceWithServer, minDifferenceWithServer);
            timestampValid |= spt.hasDisableTimestampCheck();

            if (!timestampValid) {
                response.type(MessageType.NAK);
            }
            // Timestamp diverges from allowed interval (by default +20/-40) and is considered as wrong message.
            if (!timestampValid && spt.hasFilterInvalidTimestamps()) {
                rctListener.onError("Invalid timestamp reception and SPT has disabled event passthrough. Discarding message.", address, message);
                listener.response(response.build());
                return;
            }


        } else {

            // When the message could be ciphered but is not, and we reject non-ciphered messages, gives no response.
            if (message.type().mayBeCiphered() && configuration.isRejectUnencryptedMessage()) {
                rctListener.onError("Invalid message. Non-ciphered and RCT is rejecting unencrypted messages.", address, message);
                listener.close();
                return;
            }

            // When the message could be ciphered and the SPT has ciphering configured, gives no response.
            if (message.type().mayBeCiphered() && spt.getParameters().usesCiphering()) {
                rctListener.onError("Invalid message. Non-ciphered and SPT is configured to use ciphering.", address, message);
                listener.close();
                return;
            }

            // Deals with non-ciphered message with timestamps.
            if (message.getTimestamp().isPresent()) {
                boolean timestampValid = isTimestampValid(message, reception, maxDifferenceWithServer, minDifferenceWithServer);
                if (!timestampValid) {
                    LOG.info("Invalid timestamp with non-ciphered SPT. Accepting message, might want to check SPT configuration.");
                }

                // When a non-ciphered message is reception with a timestamp, responds with a timestamp.
                response.timestamp(now);
            }
        }

        // Rejects non-events and non-polling messages. Calls polling method and responds with acknowledge.
        // Continue with events.
        switch (message.type()) {
            case EVENT:
                break;
            case NULL:
                rctListener.onPolling((Polling) message, spt, rct, address, response, listener);
                return;
            default:
                rctListener.onError("Unknown message reception.", address, message);
                return;
        }

        rctListener.onEvent((Event) message, spt, rct, address, response, listener);

    }

    @Override
    public void onError(final Dc09Exception exception, final RemoteAddressResolver address) {

        rctListener.onError("SPT: " + Throwables.getRootCause(exception).getMessage(), address, exception);

    }

    @Override
    public void onByteLog(final byte[] message,
                          final RemoteAddressResolver address,
                          final Optional<Dc09Spt> sptDc09,
                          final Direction direction) {
        LOG.info("Byte {} [{}] on [{}] by [{}].", direction(direction), bytesToAsciiString(message), address, spt(sptDc09));
    }

    @Override
    public void onMessageLog(final Message message,
                             final RemoteAddressResolver address,
                             final Optional<Dc09Spt> sptDc09,
                             final Direction direction) {
        LOG.info("Message {} [{}] on [{}] by [{}].", direction(direction), message, address, spt(sptDc09));
    }
}
