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
import ch.swissdotnet.siadc09.TransportParameters;
import ch.swissdotnet.siadc09.exceptions.Dc09Exception;
import ch.swissdotnet.siadc09.exceptions.InvalidCipheringException;
import ch.swissdotnet.siadc09.messages.DataMessage;
import ch.swissdotnet.siadc09.messages.Event;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.messages.Polling;
import ch.swissdotnet.siadc09.messages.encryption.AesCbcCipherAlgorithm;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.parameters.Dc09SptParameters;
import ch.swissdotnet.siadc09.server.RctDc09;
import ch.swissdotnet.siadc09.server.SiaDc09Servers;
import ch.swissdotnet.siadc09.server.listeners.RctListener;
import ch.swissdotnet.siadc09.server.listeners.RctMessageFilter;
import ch.swissdotnet.siadc09.server.listeners.ServerResponseListener;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;


public class BothTcpUdpServerTestManual {

    // SLF4J Logger
    private static final Logger LOG = LoggerFactory.getLogger(BothTcpUdpServerTestManual.class);

    public static void main(String[] args) throws InterruptedException, InvalidCipheringException, IOException {
        new BothTcpUdpServerTestManual().fireUpServer();
    }

    public void fireUpServer() throws IOException, InterruptedException, InvalidCipheringException {
        IncomingMessageLogger msgLogger = IncomingMessageLogger.fromConfig();
        msgLogger.open();

        Dc09TestSptStore store = new Dc09TestSptStore();
        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        Dc09Spt spt = Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build();
        store.parameters.put("080027E62A64", spt);

        Dc09GlobalParameters globalParameters = new Dc09GlobalParameters();

        // DSC TL-series binary wrapper decoder.
        // Uses default all-zero AES key (00000000000000000000000000000000) unless overridden.
        // Set skipCrcValidation=true if you want to test without confirming CRC byte order.
        DscFrameDecoder dscDecoder = new DscFrameDecoder();

        SiaDc09Servers servers = new SiaDc09Servers(
            Lists.newArrayList(
                RctDc09.newRctDc09("0.0.0.0", 50005, RctDc09.Transport.BOTH).build(),
                RctDc09.newRctDc09("0.0.0.0", 50006, RctDc09.Transport.BOTH).build()
            ),
            new TransportParameters().setLogMessages(true),
            store,
            new Dc09GlobalParameters(),
            Optional.empty(),
            Optional.empty(),
            dscDecoder
        );

        servers.addMessageListener(
            new RctMessageFilter(
                globalParameters, new RctListener() {
                @Override
                public void onPolling(final Polling polling,
                                      final Dc09Spt spt,
                                      final RctDc09 rct,
                                      final RemoteAddressResolver address,
                                      final Message.Builder response,
                                      final ServerResponseListener responseListener) {
                    printMessage("POLL  from " + address.getHost() + ":" + address.getPort(), polling);
                }

                @Override
                public void onEvent(final Event event,
                                    final Dc09Spt spt,
                                    final RctDc09 rct,
                                    final RemoteAddressResolver address,
                                    final Message.Builder response,
                                    final ServerResponseListener responseListener) {
                    printMessage("EVENT from " + address.getHost() + ":" + address.getPort(), event);
                    responseListener.response(response.build());
                }

                @Override
                public void onError(final String error,
                                    final RemoteAddressResolver address,
                                    final Message message) {
                    printError(error, address.getHost() + ":" + address.getPort());
                }

                @Override
                public void onError(final String error,
                                    final RemoteAddressResolver address,
                                    final Dc09Exception exception) {
                    printError(error + " — " + exception.getMessage(), address.getHost() + ":" + address.getPort());
                }

            }
            ) {
                @Override
                public void onByteLog(final byte[] message,
                                      final RemoteAddressResolver address,
                                      final Optional<Dc09Spt> sptDc09,
                                      final Direction direction) {
                    super.onByteLog(message, address, sptDc09, direction);
                    if (direction == Direction.INCOMING) {
                        msgLogger.log(
                            address.getHost() + ":" + address.getPort(),
                            Dc09Utils.bytesToAsciiString(message));
                    }
                }

                @Override
                public void onMessageLog(final Message message,
                                         final RemoteAddressResolver address,
                                         final Optional<Dc09Spt> sptDc09,
                                         final Direction direction) {
                    super.onMessageLog(message, address, sptDc09, direction);
                    String arrow = direction == Direction.INCOMING ? "→ IN " : "← OUT";
                    printMessage(arrow + "  " + address.getHost() + ":" + address.getPort(), message);
                }
            }
        );

        Thread thread = new Thread(servers);

        thread.start();

        printBanner(msgLogger.isEnabled());

        int read = System.in.read();
        servers.setLogBytes(true).setLogMessages(true);
        msgLogger.setEnabled(true);
        System.out.println("  [byte+message logging ON, file logging ON — Enter again to disable]");

        read = System.in.read();
        servers.setLogBytes(false).setLogMessages(false);
        msgLogger.setEnabled(false);
        System.out.println("  [logging OFF — Enter again to stop server]");

        read = System.in.read();
        msgLogger.close();
        servers.close();

        thread.join();
    }

    // ── Pretty-print helpers ──────────────────────────────────────────────────

    private static void printBanner(final boolean fileLogEnabled) {
        String fileLog = fileLogEnabled ? "ON  (incoming-messages.log)" : "OFF (see server-log.properties)";
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║   SIA DC-09 Server  —  BothTcpUdp Mode            ║");
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║  Listening on  0.0.0.0:50005   (TCP+UDP)           ║");
        System.out.println("║  Listening on  0.0.0.0:50006   (TCP+UDP)           ║");
        System.out.println("║  Account       080027E62A64                        ║");
        System.out.println("║  Cipher        AES-CBC                             ║");
        System.out.printf( "║  File logging  %-35s║%n", fileLog);
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║  Enter #1 → enable logging + file logging          ║");
        System.out.println("║  Enter #2 → disable logging + file logging         ║");
        System.out.println("║  Enter #3 → stop server                            ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static void printMessage(final String label, final Message msg) {
        String type    = msg.type() != null ? msg.type().name() : "?";
        String account = msg.getAccountNumber();
        String seq     = String.valueOf(msg.getSequence());
        String cipher  = msg.isCiphered() ? "yes" : "no";
        String time    = msg.getTimestamp().map(Object::toString).orElse("—");
        String id      = msg.getId() != null ? msg.getId() : "—";
        String data    = (msg instanceof DataMessage dm) ? dm.getData() : "—";
        String ts      = java.time.LocalTime.now().toString().substring(0, 12);

        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────┐");
        System.out.printf( "│  [%s]  %-34s│%n", ts, label);
        System.out.println("├─────────────────────────────────────────────────┤");
        System.out.printf( "│  Type    : %-36s│%n", type);
        System.out.printf( "│  Account : %-36s│%n", account);
        System.out.printf( "│  Seq     : %-36s│%n", seq);
        System.out.printf( "│  ID      : %-36s│%n", id);
        System.out.printf( "│  Data    : %-36s│%n", data);
        System.out.printf( "│  Ciphered: %-36s│%n", cipher);
        System.out.printf( "│  MsgTime : %-36s│%n", time);
        System.out.println("└─────────────────────────────────────────────────┘");
    }

    private static void printError(final String error, final String address) {
        String ts = java.time.LocalTime.now().toString().substring(0, 12);
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────┐");
        System.out.printf( "│  [%s]  ✗ ERROR from %-26s│%n", ts, address);
        System.out.println("├─────────────────────────────────────────────────┤");
        // wrap error text at 47 chars per line
        String remaining = error;
        while (!remaining.isEmpty()) {
            String chunk = remaining.length() > 47 ? remaining.substring(0, 47) : remaining;
            System.out.printf("│  %-47s│%n", chunk);
            remaining = remaining.length() > 47 ? remaining.substring(47) : "";
        }
        System.out.println("└─────────────────────────────────────────────────┘");
    }
}
