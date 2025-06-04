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
import ch.swissdotnet.siadc09.messages.Polling;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.server.RctDc09;

/**
 * The {@code RctListener} interface allows for SIA DC-09 message handling by RCT.
 * <p/>
 * Once the message constraints filtered the {@code Message}, the dispatch is done
 * for the RCT by either calling {@link #onEvent(Event, Dc09Spt, RctDc09, RemoteAddressResolver, Message.Builder, ServerResponseListener)}
 * or {@link #onPolling(Polling, Dc09Spt, RctDc09, RemoteAddressResolver, Message.Builder, ServerResponseListener)}.
 * </p>
 * When an error occurs during message handling, the RCT is notified through either {@link #onError(String, RemoteAddressResolver, Message)}
 * or {@link #onError(String, RemoteAddressResolver, Dc09Exception)}.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public interface RctListener {

    /**
     * Received a {@code Polling} message from given SPT on RCT.
     *
     * @param polling          the polling received
     * @param spt              the SPT which sent the polling message
     * @param rct              the RCT involved in the exchange
     * @param address          the address on which the message is received
     * @param response         the response builder with pre-filled values
     * @param responseListener the listener to send response to
     *
     * @see RctMessageFilter
     */
    void onPolling(final Polling polling,
                   final Dc09Spt spt,
                   final RctDc09 rct,
                   final RemoteAddressResolver address,
                   final Message.Builder response,
                   final ServerResponseListener responseListener);

    /**
     * Received a {@code Event} message from given SPT on RCT.
     *
     * @param event            the event received
     * @param spt              the SPT which sent the polling message
     * @param rct              the RCT involved in the exchange
     * @param address          the address on which the message is received
     * @param response         the response builder with pre-filled values
     * @param responseListener the listener to send response to
     */
    void onEvent(final Event event,
                 final Dc09Spt spt,
                 final RctDc09 rct,
                 final RemoteAddressResolver address,
                 final Message.Builder response,
                 final ServerResponseListener responseListener);

    /**
     * Received a {@code Message} but filtering indicated an error.
     *
     * @param error   the error involving the message
     * @param address the address on which the message in error is received
     * @param message the message in error
     */
    void onError(final String error,
                 final RemoteAddressResolver address,
                 final Message message);

    /**
     * Received a {@code Dc09Exception} while treating message.
     *
     * @param error     the error involving the message
     * @param address   the address on which the message in error is received
     * @param exception the exception
     */
    void onError(final String error,
                 final RemoteAddressResolver address,
                 final Dc09Exception exception);

}
