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
        Dc09TestSptStore store = new Dc09TestSptStore();
        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        Dc09Spt spt = Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build();
        store.parameters.put("080027E62A64", spt);

        Dc09GlobalParameters globalParameters = new Dc09GlobalParameters();

        SiaDc09Servers servers = new SiaDc09Servers(
            Lists.newArrayList(
                RctDc09.newRctDc09("0.0.0.0", 33200, RctDc09.Transport.BOTH).build(),
                RctDc09.newRctDc09("0.0.0.0", 33201, RctDc09.Transport.BOTH).build()
            ),
            new TransportParameters().setLogMessages(true),
            store,
            new Dc09GlobalParameters(),
            Optional.empty(),
            Optional.empty()
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
                    System.out.println(polling);
                }

                @Override
                public void onEvent(final Event event,
                                    final Dc09Spt spt,
                                    final RctDc09 rct,
                                    final RemoteAddressResolver address,
                                    final Message.Builder response,
                                    final ServerResponseListener responseListener) {
                    System.out.println(event);
                    responseListener.response(response.build());
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
            ) {
                @Override
                public void onByteLog(final byte[] message,
                                      final RemoteAddressResolver address,
                                      final Optional<Dc09Spt> sptDc09,
                                      final Direction direction) {
                    super.onByteLog(message, address, sptDc09, direction);
                }

                @Override
                public void onMessageLog(final Message message,
                                         final RemoteAddressResolver address,
                                         final Optional<Dc09Spt> sptDc09,
                                         final Direction direction) {
                    super.onMessageLog(message, address, sptDc09, direction);
                    LOG.info("{} from {} with {}.", direction, address, message);
                }
            }
        );

        Thread thread = new Thread(servers);

        thread.start();

        int read = System.in.read();
        System.out.println(read);

        servers.setLogBytes(true).setLogMessages(true);

        read = System.in.read();
        System.out.println(read);

        servers.setLogBytes(false).setLogMessages(false);

        read = System.in.read();
        System.out.println(read);

        servers.close();

        thread.join();
    }
}
