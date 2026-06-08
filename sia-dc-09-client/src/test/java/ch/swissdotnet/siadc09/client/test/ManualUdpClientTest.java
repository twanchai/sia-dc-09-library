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
import ch.swissdotnet.siadc09.messages.DataMessage;
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
        // Load client config
        java.util.Properties cfg = new java.util.Properties();
        try (java.io.InputStream is = getClass().getClassLoader()
                .getResourceAsStream("sia-dc-09-client.properties")) {
            if (is != null) cfg.load(is);
        }
        String host = cfg.getProperty("client.server.host", "localhost");
        int    port = Integer.parseInt(cfg.getProperty("client.server.port", "50005"));

        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        Dc09Spt spt1 = Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build();

        TniDc09 tni = TniDc09.newUdpAtp(HostAndPort.fromParts(host, port))
            .withTimeoutInSeconds(5)
            .build();

        Message.Builder builder1 = Message.newEventBuilder("SIA-DCS", "NYS0021").ciphered(true);
        Message.Builder builder2 = Message.newEventBuilder("SIA-DCS", "NYK0021").ciphered(true);
        Message.Builder builder3 = Message.newEventBuilder("SIA-DCS", "NFA0002").ciphered(true);

        ClientResponseListener listener = new MyClientResponseListener();
        TniDc09Client client1 = tni.sender(listener).get();

        printMenu(host, port);
        Scanner scanner = new Scanner(System.in);
        String line;
        while ((line = scanner.nextLine()) != null) {
            switch (line) {
                case "1" -> {
                    builder1.timestamp(DateTime.now(DateTimeZone.UTC));
                    client1.send(spt1, builder1);
                    LOG.info("→ Sent NYS0021 (alarm event)");
                }
                case "2" -> {
                    builder2.timestamp(DateTime.now(DateTimeZone.UTC));
                    client1.send(spt1, builder2);
                    LOG.info("→ Sent NYK0021 (keypad event)");
                }
                case "3" -> {
                    builder3.timestamp(DateTime.now(DateTimeZone.UTC));
                    client1.send(spt1, builder3);
                    LOG.info("→ Sent NFA0002 (fire alarm)");
                }
                case "q" -> {
                    LOG.info("Quit");
                    System.exit(0);
                }
                default -> {
                    LOG.info("Unknown command: '{}'", line);
                    printMenu(host, port);
                }
            }
        }
    }

    private static void printMenu(final String host, final int port) {
        System.out.println();
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│  SIA DC-09 Manual Client                │");
        System.out.printf( "│  Target: %-31s│%n", host + ":" + port + " (UDP)");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  1 → Send NYS0021 (alarm event)         │");
        System.out.println("│  2 → Send NYK0021 (keypad event)        │");
        System.out.println("│  3 → Send NFA0002 (fire alarm)          │");
        System.out.println("│  q → Quit                               │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("Enter command: ");
    }

    private static void printMessage(final String label, final Message msg) {
        String type    = msg.type() != null ? msg.type().name() : "?";
        String account = msg.getAccountNumber();
        String seq     = String.valueOf(msg.getSequence());
        String time    = msg.getTimestamp().map(Object::toString).orElse("—");
        String id      = msg.getId() != null ? msg.getId() : "—";
        String data    = (msg instanceof DataMessage dm) ? dm.getData() : "—";

        System.out.println();
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.printf( "│  %-41s│%n", label);
        System.out.println("├─────────────────────────────────────────┤");
        System.out.printf( "│  Type    : %-30s│%n", type);
        System.out.printf( "│  Account : %-30s│%n", account);
        System.out.printf( "│  Seq     : %-30s│%n", seq);
        System.out.printf( "│  ID      : %-30s│%n", id);
        System.out.printf( "│  Data    : %-30s│%n", data);
        System.out.printf( "│  Time    : %-30s│%n", time);
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("Enter command: ");
    }

    private static class MyClientResponseListener implements ClientResponseListener {

        @Override
        public void onResponse(final Message response, final Message message) {
            printMessage("SERVER RESPONSE", response);
        }

        @Override
        public void onDisconnect(final TniDc09 tni) {
            System.out.println();
            System.out.println("┌─────────────────────────────────────────┐");
            System.out.println("│  ⚠  DISCONNECTED (server closed)        │");
            System.out.println("└─────────────────────────────────────────┘");
        }

        @Override
        public void onMessageTimeout(final Message message) {
            System.out.println();
            System.out.println("┌─────────────────────────────────────────┐");
            System.out.printf( "│  ⚠  TIMEOUT — no reply for seq %-8s│%n", message.getSequence());
            System.out.println("└─────────────────────────────────────────┘");
            System.out.print("Enter command: ");
        }

        @Override
        public void onError(final Dc09Exception exception,
                            final RemoteAddressResolver address) {
            System.out.println();
            System.out.println("┌─────────────────────────────────────────┐");
            System.out.printf( "│  ✗  ERROR: %-30s│%n", exception.getMessage());
            System.out.println("└─────────────────────────────────────────┘");
            System.out.print("Enter command: ");
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
                printMessage("← INCOMING from " + address.getHost() + ":" + address.getPort(), message);
            }
        }
    }
}
