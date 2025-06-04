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
package ch.swissdotnet.siadc09.client.test;

import ch.swissdotnet.siadc09.Dc09Utils;
import ch.swissdotnet.siadc09.RemoteAddressResolver;
import ch.swissdotnet.siadc09.client.ClientResponseListener;
import ch.swissdotnet.siadc09.client.TniDc09;
import ch.swissdotnet.siadc09.client.TniDc09Client;
import ch.swissdotnet.siadc09.exceptions.Dc09Exception;
import ch.swissdotnet.siadc09.exceptions.InvalidCipheringException;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.messages.encryption.AesCbcCipherAlgorithm;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.parameters.Dc09SptParameters;
import com.google.common.net.HostAndPort;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;


public class ManualUdpClientTest {

    // SLF4J Logger
    private static final Logger LOG = LoggerFactory.getLogger(ManualUdpClientTest.class);

    public static void main(String[] args) {
        ManualUdpClientTest manualUdpClientTest = new ManualUdpClientTest();
        try {
            manualUdpClientTest.createClientSendEvent();
        } catch (InvalidCipheringException | IOException e) {
            e.printStackTrace();
        }
    }

    @Disabled
    @Test
    public void createClientSendEvent() throws InvalidCipheringException, IOException {
        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        Dc09Spt spt1 = Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build();

        TniDc09 tni = TniDc09.newUdpAtp(HostAndPort.fromParts("127.0.0.1", 33200))
            .withTimeoutInSeconds(5)
            .build();

        Message.Builder builder1 = Message.newEventBuilder("SIA-DCS", "NYS0021").ciphered(true);
        Message.Builder builder2 = Message.newEventBuilder("SIA-DCS", "NYK0021").ciphered(true);
        Message.Builder builder3 = Message.newEventBuilder("SIA-DCS", "NFA0002").ciphered(true);

        ClientResponseListener listener = new MyClientResponseListener();
        TniDc09Client client1 = tni.sender(listener).get();

        Scanner scanner = new Scanner(System.in);
        String line;
        while ((line = scanner.nextLine()) != null) {
            switch (line) {
                case "1" -> {
                    builder1.timestamp(DateTime.now(DateTimeZone.UTC));
                    client1.send(spt1, builder1);
                }
                case "2" -> {
                    builder2.timestamp(DateTime.now(DateTimeZone.UTC));
                    client1.send(spt1, builder2);
                }
                case "3" -> {
                    builder3.timestamp(DateTime.now(DateTimeZone.UTC));
                    client1.send(spt1, builder3);
                }
                case "q" -> {
                    LOG.info("Quit");
                    System.exit(0);
                }
                default -> LOG.info("No.");
            }
        }
    }

    private static class MyClientResponseListener implements ClientResponseListener {

        @Override
        public void onResponse(final Message response, final Message message) {
            System.out.println(response);
        }

        @Override
        public void onDisconnect(final TniDc09 tni) {
            System.out.println("Timeout");
        }

        @Override
        public void onMessageTimeout(final Message message) {
            System.out.println("Timeout");
        }

        @Override
        public void onError(final Dc09Exception exception,
                            final RemoteAddressResolver address) {

        }

        @Override
        public void onByteLog(final byte[] message,
                              final RemoteAddressResolver address,
                              final Optional<Dc09Spt> sptDc09,
                              final Direction direction) {

        }

        @Override
        public void onMessageLog(final Message message,
                                 final RemoteAddressResolver address,
                                 final Optional<Dc09Spt> sptDc09,
                                 final Direction direction) {
            if (direction == Direction.INCOMING) {
                System.out.println(message);
            }
        }
    }
}
