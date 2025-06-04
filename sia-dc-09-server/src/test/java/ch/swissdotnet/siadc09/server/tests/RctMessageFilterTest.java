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
package ch.swissdotnet.siadc09.server.tests;

import ch.swissdotnet.siadc09.Dc09Utils;
import ch.swissdotnet.siadc09.RemoteAddressResolver;
import ch.swissdotnet.siadc09.exceptions.Dc09Exception;
import ch.swissdotnet.siadc09.exceptions.InvalidCipheringException;
import ch.swissdotnet.siadc09.messages.Event;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.messages.MessageType;
import ch.swissdotnet.siadc09.messages.Polling;
import ch.swissdotnet.siadc09.messages.encryption.AesCbcCipherAlgorithm;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.parameters.Dc09SptParameters;
import ch.swissdotnet.siadc09.server.RctDc09;
import ch.swissdotnet.siadc09.server.listeners.RctListener;
import ch.swissdotnet.siadc09.server.listeners.RctMessageFilter;
import ch.swissdotnet.siadc09.server.listeners.ServerResponseListener;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class RctMessageFilterTest {

    private final Dc09GlobalParameters internalConfiguration = new Dc09GlobalParameters();
    private final RctDc09 rct = RctDc09.newRctDc09("0.0.0.0", 12345, RctDc09.Transport.TCP).build();
    private final Dc09Spt sptUnciphered = Dc09Spt.newSptBuilder("1234", new Dc09SptParameters()).build();
    private final Dc09Spt sptCiphered;
    private final Dc09Spt sptCipheredFilterTimestamps;
    private final RemoteAddressResolver fake = new RemoteAddressResolver.InetSocketAddressResolver(new InetSocketAddress(0));
    private final RctMessageFilter listener;
    private ServerResponseListenerInstrumented responseListener;

    public RctMessageFilterTest() throws InvalidCipheringException {
        sptCiphered = Dc09Spt.newSptBuilder(
            "5678", new Dc09SptParameters(
                new AesCbcCipherAlgorithm(Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD"))
            )
        ).build();
        sptCipheredFilterTimestamps = Dc09Spt.newSptBuilder(
                "5678",
                new Dc09SptParameters(new AesCbcCipherAlgorithm(Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD")))
            )
            .filterInvalidTimestamps(true)
            .build();
        listener = new RctMessageFilter(
            internalConfiguration,
            new RctListenerInstrumented()
        );
    }

    @Test
    public void invalidMessage() throws InterruptedException {
        responseListener = new ServerResponseListenerInstrumented(0, 1);
        listener.onMessage(
            Message.newDuhBuilder().actualCrc(new byte[1]).readCrc(new byte[2]).build(),
            DateTime.now(),
            sptUnciphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.noResponseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNull(responseListener.message);
    }

    @Test
    public void cipheredNoTimestampNoNakIsAck() throws InterruptedException {
        responseListener = new ServerResponseListenerInstrumented(0, 1);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001").ciphered(true).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptCipheredFilterTimestamps,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.noResponseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNull(responseListener.message);
    }

    @Test
    public void unCipheredRejectsUnencrypted() throws InterruptedException {
        internalConfiguration.setRejectUnencryptedMessage(true);
        responseListener = new ServerResponseListenerInstrumented(0, 1);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001").actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptCiphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.noResponseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNull(responseListener.message);
    }

    @Test
    public void unCipheredInvalidTimestamp() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration.setRejectUnencryptedMessage(false),
            new RctListenerInstrumented(MessageType.ACK)
        );
        sptUnciphered.setDisableTimestampChecks(true);
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001")
                .timestamp(DateTime.now().minusYears(1)).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptUnciphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.ACK, responseListener.message.type());
    }

    @Test
    public void unCipheredValidMessage() throws InterruptedException {
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001")
                .timestamp(DateTime.now()).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptUnciphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.ACK, responseListener.message.type());
    }

    @Test
    public void unCipheredValidMessageDuh() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration.setRejectUnencryptedMessage(false),
            new RctListenerInstrumented(MessageType.DUH)
        );
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001")
                .timestamp(DateTime.now()).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptUnciphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.DUH, responseListener.message.type());
    }

    @Test
    public void unCipheredWithCipheredSpt() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration.setRejectUnencryptedMessage(false),
            new RctListenerInstrumented(MessageType.DUH)
        );
        responseListener = new ServerResponseListenerInstrumented(0, 1);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001")
                .timestamp(DateTime.now()).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptCiphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.noResponseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNull(responseListener.message);
    }

    @Test
    public void cipheredInvalidTimestampNoNakIsAck() throws InterruptedException {
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001")
                .timestamp(DateTime.now().minusYears(1))
                .ciphered(true).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptCipheredFilterTimestamps,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.NAK, responseListener.message.type());
    }

    @Test
    public void cipheredInvalidTimestampNakIsAck() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration,
            new RctListenerInstrumented(MessageType.ACK)
        );
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001")
                .timestamp(DateTime.now().minusYears(1))
                .ciphered(true).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptCiphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.ACK, responseListener.message.type());
    }

    @Test
    public void cipheredInvalidTimestampWithTimestampChecks() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration,
            new RctListenerInstrumented(MessageType.ACK)
        );
        sptCiphered.setDisableTimestampChecks(true);
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001")
                .timestamp(DateTime.now().minusYears(1))
                .ciphered(true).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptCiphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.ACK, responseListener.message.type());
    }

    @Test
    public void cipheredInvalidTimestampWithTimestampChecksAndEventPassthrough() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration,
            new RctListenerInstrumented(MessageType.ACK)
        );
        sptCiphered.setFilterInvalidTimestamps(true)
            .setDisableTimestampChecks(true);
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001")
                .timestamp(DateTime.now().minusYears(1))
                .ciphered(true).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptCiphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.ACK, responseListener.message.type());
    }

    @Test
    public void cipheredValidMessage() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration,
            new RctListenerInstrumented(MessageType.ACK)
        );
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001")
                .timestamp(DateTime.now())
                .ciphered(true).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptCiphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.ACK, responseListener.message.type());
    }

    @Test
    public void cipheredValidMessageDuh() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration,
            new RctListenerInstrumented(MessageType.DUH)
        );
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newEventBuilder("SIA-DCS", "NFA001")
                .timestamp(DateTime.now())
                .ciphered(true).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptCiphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.DUH, responseListener.message.type());
    }

    @Test
    public void uncipheredValidPolling() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration.setRejectUnencryptedMessage(false),
            new RctListenerInstrumented(MessageType.ACK)
        );
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newPollingBuilder()
                .timestamp(DateTime.now())
                .ciphered(false).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptUnciphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.ACK, responseListener.message.type());
        Assertions.assertEquals(0, responseListener.message.getSequence());
    }

    @Test
    public void uncipheredValidPollingWithSequence() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration.setRejectUnencryptedMessage(false),
            new RctListenerInstrumented(MessageType.ACK)
        );
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newPollingBuilder()
                .timestamp(DateTime.now())
                .sequence(1)
                .ciphered(false).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptUnciphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.ACK, responseListener.message.type());
        Assertions.assertEquals(1, responseListener.message.getSequence());
    }

    @Test
    public void cipheredValidPollingWithSequence() throws InterruptedException {
        RctMessageFilter listener = new RctMessageFilter(
            internalConfiguration,
            new RctListenerInstrumented(MessageType.ACK)
        );
        responseListener = new ServerResponseListenerInstrumented(1, 0);
        listener.onMessage(
            Message.newPollingBuilder()
                .timestamp(DateTime.now())
                .sequence(1)
                .ciphered(true).actualCrc(new byte[1]).readCrc(new byte[1]).build(),
            DateTime.now(),
            sptCiphered,
            rct,
            fake,
            responseListener
        );

        boolean awaited = responseListener.responseLatch.await(100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(awaited);
        Assertions.assertNotNull(responseListener.message);
        Assertions.assertEquals(MessageType.ACK, responseListener.message.type());
        Assertions.assertEquals(1, responseListener.message.getSequence());
    }

    private static class ServerResponseListenerInstrumented implements ServerResponseListener {

        protected final CountDownLatch responseLatch;
        protected final CountDownLatch noResponseLatch;
        protected Message message;

        public ServerResponseListenerInstrumented(final int response, final int noResponse) {
            responseLatch = new CountDownLatch(response);
            noResponseLatch = new CountDownLatch(noResponse);
        }

        @Override
        public void response(final Message message) {
            responseLatch.countDown();
            this.message = message;
        }

        @Override
        public void close() {
            noResponseLatch.countDown();
        }
    }

    private static class RctListenerInstrumented implements RctListener {

        private final MessageType responseType;

        public RctListenerInstrumented() {
            this.responseType = MessageType.ACK;
        }

        public RctListenerInstrumented(final MessageType responseType) {
            this.responseType = responseType;
        }

        @Override
        public void onPolling(final Polling polling,
                              final Dc09Spt spt,
                              final RctDc09 rct,
                              final RemoteAddressResolver address,
                              final Message.Builder response,
                              final ServerResponseListener responseListener) {
            responseListener.response(response.type(responseType).build());
        }

        @Override
        public void onEvent(final Event event,
                            final Dc09Spt spt,
                            final RctDc09 rct,
                            final RemoteAddressResolver address,
                            final Message.Builder response,
                            final ServerResponseListener responseListener) {
            responseListener.response(response.type(responseType).build());
        }

        @Override
        public void onError(final String error,
                            final RemoteAddressResolver address,
                            final Message message) {

        }

        @Override
        public void onError(final String error,
                            final RemoteAddressResolver address,
                            final Dc09Exception exception) {

        }
    }
}
