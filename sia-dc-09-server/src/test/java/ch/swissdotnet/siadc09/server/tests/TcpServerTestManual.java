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
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.messages.encryption.AesCbcCipherAlgorithm;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.parameters.Dc09SptParameters;
import ch.swissdotnet.siadc09.server.RctDc09;
import ch.swissdotnet.siadc09.server.SiaDc09Server;
import ch.swissdotnet.siadc09.server.SiaDc09Servers;
import ch.swissdotnet.siadc09.server.impl.TcpSiaDc09Server;
import ch.swissdotnet.siadc09.server.listeners.ServerMessageListener;
import ch.swissdotnet.siadc09.server.listeners.ServerResponseListener;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class TcpServerTestManual {

    public static void main(String[] args) throws Exception {
        new TcpServerTestManual().fireUpServer();
    }

    public void fireUpServer() throws Exception {
        Dc09TestSptStore store = new Dc09TestSptStore();
        byte[] hexKey = Dc09Utils.hexStringToBytes("ABCDABCDABCDABCDABCDABCDABCDABCD");
        Dc09Spt spt = Dc09Spt.newSptBuilder("080027E62A64", new Dc09SptParameters(new AesCbcCipherAlgorithm(hexKey))).build();
        store.parameters.put("080027E62A64", spt);

        ExecutorService onMessageExecutor = new ThreadPoolExecutor(
            0,
            10,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new SiaDc09Servers.SiaThreadFactory("SIA-DC-09-onMessage")
        );
        SiaDc09Server server = new TcpSiaDc09Server(
            RctDc09.newRctDc09("0.0.0.0", 50005, RctDc09.Transport.TCP).build(),
            new TransportParameters(),
            onMessageExecutor,
            new Dc09GlobalParameters(),
            store
        );

        server.addMessageListener(
            new ServerMessageListener() {
                @Override
                public void onMessage(final Message message,
                                      final DateTime received,
                                      final Dc09Spt spt,
                                      final RctDc09 rct,
                                      final RemoteAddressResolver address,
                                      final ServerResponseListener listener) {

                    Message ack = Message.newAckBuilder()
                        .accountNumber(message.getAccountNumber())
                        .build();
                    listener.response(ack);
                }

                @Override
                public void onError(final Dc09Exception exception,
                                    final RemoteAddressResolver address) {
                    System.out.println(exception.getAccountNumber().isPresent() ? exception.getAccountNumber().get() : "");
                    System.out.println(exception.getMessage());
                }

                @Override
                public void onByteLog(final byte[] message,
                                      final RemoteAddressResolver address,
                                      final Optional<Dc09Spt> sptDc09,
                                      final Direction direction) {
                    System.out.println(Dc09Utils.bytesToAsciiString(message));
                }

                @Override
                public void onMessageLog(final Message message,
                                         final RemoteAddressResolver address,
                                         final Optional<Dc09Spt> sptDc09,
                                         final Direction direction) {
                    System.out.println(message.toString());
                }
            }
        );

        Thread thread = new Thread(server);

        thread.start();

        int read = System.in.read();
        System.out.println(read);

        server.setLogBytes(true).setLogMessages(true);

        read = System.in.read();
        System.out.println(read);

        server.setLogBytes(false).setLogMessages(false);

        read = System.in.read();
        System.out.println(read);

        server.close();

        thread.join();

    }

}

