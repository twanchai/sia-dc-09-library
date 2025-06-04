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
package ch.swissdotnet.siadc09.client.impl;

import ch.swissdotnet.siadc09.CyclicAtomicCounter;
import ch.swissdotnet.siadc09.client.SenderParameters;
import ch.swissdotnet.siadc09.client.TniDc09Client;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import io.netty.channel.Channel;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class TcpUdpDc09Client implements TniDc09Client {

    private final SenderParameters senderParameters;
    private final SptStore store;
    private final Channel channel;
    private final CyclicAtomicCounter sequence;

    public TcpUdpDc09Client(final SenderParameters senderParameters,
                            final SptStore store,
                            final Channel channel,
                            final CyclicAtomicCounter sequence) {
        this.senderParameters = senderParameters;
        this.store = store;
        this.channel = channel;
        this.sequence = sequence;
    }

    @Override
    public Message send(final Dc09Spt spt, final Message.Builder builder) {
        store.addSpt(spt);
        if (senderParameters.isOverrideSequence()) {
            int id = next();
            builder.sequence(id);
        }
        if (senderParameters.isOverrideAccountNumber()) builder.accountNumber(spt.getAccountNumber());
        if (senderParameters.isOverrideAccountDataNumber()) builder.accountNumberData(spt.getAccountNumber());
        if (senderParameters.isOverrideAccountPrefix()) builder.accountPrefix(spt.getAccountPrefix());
        if (senderParameters.isOverrideReceiverNumber()) builder.receiverNumber(spt.getReceiverNumber());
        if (senderParameters.isOverrideTimestamp()) builder.timestamp(DateTime.now(DateTimeZone.UTC));
        Message message = builder.build();
        channel.writeAndFlush(new MessageHandler.MessageHolder(spt, message));

        return message;
    }

    public int next() {
        return sequence.getAndIncrement();
    }

    @Override
    public void close() {
        channel.close();
    }
}
