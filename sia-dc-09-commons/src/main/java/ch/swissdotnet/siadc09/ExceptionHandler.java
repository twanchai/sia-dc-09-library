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
package ch.swissdotnet.siadc09;

import ch.swissdotnet.siadc09.exceptions.Dc09Exception;
import ch.swissdotnet.siadc09.exceptions.InvalidMessageException;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import com.google.common.base.Throwables;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ch.swissdotnet.siadc09.ChannelUtilities.REMOTE_ADDRESS_RESOLVER;

/**
 * The {@code ExceptionHandler} class catches incoming and outgoing {@code Exception} thrown on the {@code Channel} pipeline.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
@ChannelHandler.Sharable
public final class ExceptionHandler extends ChannelDuplexHandler {

    // Listeners to call upon exception handling.
    private final Set<? extends MessageListener> listeners;

    /**
     * Initializes a new {@code ExceptionHandler} with given listeners.
     *
     * @param listeners the listeners to notify upon exception reception
     */
    public ExceptionHandler(final Set<? extends MessageListener> listeners) {
        this.listeners = listeners;
    }

    private static <T> Optional<T> optException(final List<Throwable> causes,
                                                final Class<T> $class) {
        Optional<T> response = Optional.empty();
        for (Throwable cause : causes) {
            if (cause != null && $class.isAssignableFrom(cause.getClass())) {
                @SuppressWarnings ("unchecked") // We checked it can be assigned as $class.
                T cast = (T) cause;
                response = Optional.of(cast);
                break;
            }
        }
        return response;
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {

        String accountNumber = null;
        Dc09Spt dc09Spt = null;
        if (ctx.channel().attr(ChannelUtilities.SPT) != null) {
            dc09Spt = ctx.channel().attr(ChannelUtilities.SPT).get();
            accountNumber = dc09Spt != null ? dc09Spt.getAccountNumber() : null;
        }
        RemoteAddressResolver remoteAddressResolver = null;
        if (ctx.channel().attr(REMOTE_ADDRESS_RESOLVER) != null) {
            remoteAddressResolver = ctx.channel().attr(REMOTE_ADDRESS_RESOLVER).get();
        }

        ctx.channel().disconnect();

        Throwable rootCause = Throwables.getRootCause(cause);
        List<Throwable> causes = Throwables.getCausalChain(cause);
        Dc09Exception exception;
        do {
            Optional<Dc09Exception> optDc09Cause = optException(causes, Dc09Exception.class);
            if (optDc09Cause.isPresent()) {
                exception = optDc09Cause.get();
                exception.setAccountNumber(exception.getAccountNumber().orElse(null))
                    .setSptDc09(exception.getSptDc09().orElse(null));
                break;
            }

            Optional<InvalidMessageException> optInvalidMessage = optException(causes, InvalidMessageException.class);
            if (optInvalidMessage.isPresent()) {
                InvalidMessageException invalidMessageException = optInvalidMessage.get();
                exception = new Dc09Exception(invalidMessageException.getMessage())
                    .setAccountNumber(invalidMessageException.getAccountNumber().orElse(null))
                    .setSptDc09(dc09Spt);
                break;
            }

            exception = new Dc09Exception(rootCause.getMessage())
                .setAccountNumber(accountNumber)
                .setSptDc09(dc09Spt);

        } while (false);

        for (MessageListener listener : listeners) {
            listener.onError(exception, remoteAddressResolver);
        }
    }

}
