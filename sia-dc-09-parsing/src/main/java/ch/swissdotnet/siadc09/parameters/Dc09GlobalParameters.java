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
package ch.swissdotnet.siadc09.parameters;

/**
 * The {@code Dc09GlobalParameters} class holds global read/write parameters for SIA DC-09 protocol.
 * <p/>
 * Parameters involves:
 * <ul>
 * <li>
 * {@code hexToUpper} which indicate whether messages must be printed in upper case hexadecimal or not (may influence CRC and serialization). The
 * default value is set to {@code true} which prints in upper case;
 * </li>
 * <li>
 * {@code appendPipeToPolling} which indicate whether pipe character (|) is to be appended to polling messages in ciphered mode. The default
 * value
 * is set to {@code true} which prints pipe character to ciphered polling messages;
 * </li>
 * <li>
 * {@code appendPipeToAck} which indicate whether pipe character (|) is to be appended to acknowledge messages in ciphered mode. The default
 * value
 * is set to {@code true} which prints pipe character to ciphered acknowledge messages;
 * </li>
 * <li>
 * {@code rejectUnencryptedMessage} defines whether to reject unencrypted messages or not. It may be used to only use ciphered SPT. The default value
 * is {@code false};
 * </li>
 * <li>
 * {@code handleAllAsAscii} defines whether to handle all incoming messages as ASCII data or not (does not take into account SPT UTF-8 flag). The default value is {@code false};
 * </li>
 * <li>
 * {@code maxDifferenceWithServer} defines the maximum time difference between server time and message reception (server time + max < message time).
 * The default value is 20 seconds;
 * </li>
 * <li>
 * {@code minDifferenceWithServer} defines the minimum time difference between server time and message reception (server time - max < message time).
 * The default value is 40 seconds.
 * </li>
 * </ul>
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public final class Dc09GlobalParameters {

    // Whether messages must be printed in upper case Hexadecimal or not (may influence CRC and serialization).
    private boolean hexToUpper = true;
    // Whether | (pipe) character is to be appended to polling message in ciphered mode.
    private boolean appendPipeToPolling = true;
    // Whether | (pipe) character is to be appended to ack message in ciphered mode.
    private boolean appendPipeToAck = true;
    // Whether unencrypted messages are reject or not.
    private boolean rejectUnencryptedMessage = false;
    // Whether to handle all messages as ASCII or not (data and extended data).
    private boolean handleAllAsAscii = false;
    // Maximal time difference between message reception and server time (server time + max < message time).
    private int maxDifferenceWithServer = 20;
    // Minimal time difference between message reception and server time (server time - min > message time).
    private int minDifferenceWithServer = 40;

    /**
     * @return whether messages must be printer in upper case hexadecimal or not (may influence CRC and serialization)
     */
    public boolean isHexToUpper() {
        return hexToUpper;
    }

    /**
     * Sets whether messages must be printer in upper case hexadecimal or not (may influence CRC and serialization)
     *
     * @param hexToUpper whether to print upper case hexadecimal or not
     *
     * @return the {@code Dc09GlobalParameters} instance
     */
    public Dc09GlobalParameters setHexToUpper(final boolean hexToUpper) {
        this.hexToUpper = hexToUpper;
        return this;
    }

    /**
     * @return whether to append pipe to ciphered polling messages
     */
    public boolean isAppendPipeToPolling() {
        return appendPipeToPolling;
    }

    /**
     * Sets whether to append pipe to ciphered polling messages or not.
     *
     * @param appendPipeToPolling whether to append pipe to ciphered polling messages or not
     *
     * @return the {@code Dc09GlobalParameters} instance
     */
    public Dc09GlobalParameters setAppendPipeToPolling(final boolean appendPipeToPolling) {
        this.appendPipeToPolling = appendPipeToPolling;
        return this;
    }

    /**
     * @return whether to append pipe to ciphered acknowledge messages
     */
    public boolean isAppendPipeToAck() {
        return appendPipeToAck;
    }

    /**
     * Sets whether to append pipe to ciphered acknowledge messages or not.
     *
     * @param appendPipeToAck whether to append pipe to ciphered acknowledge messages or not
     *
     * @return the {@code Dc09GlobalParameters} instance
     */
    public Dc09GlobalParameters setAppendPipeToAck(final boolean appendPipeToAck) {
        this.appendPipeToAck = appendPipeToAck;
        return this;
    }

    /**
     * @return whether to reject unencrypted messages or not
     */
    public boolean isRejectUnencryptedMessage() {
        return rejectUnencryptedMessage;
    }

    /**
     * Sets whether to reject unencrypted messages or not
     *
     * @param rejectUnencryptedMessage whether to reject unencrypted messages or not
     *
     * @return the {@code Dc09GlobalParameters} instance
     */
    public Dc09GlobalParameters setRejectUnencryptedMessage(final boolean rejectUnencryptedMessage) {
        this.rejectUnencryptedMessage = rejectUnencryptedMessage;
        return this;
    }

    /**
     * Returns whether to handle all as ASCII or not (data and extended data)
     * <p/>
     * Will override SPT {@code Dc09Spt#dataCharsetToUtf8} when used.
     *
     * @return whether to handle all as ASCII or not (data and extended data)
     */
    public boolean isHandleAllAsAscii() {
        return handleAllAsAscii;
    }

    /**
     * Sets whether to handle all as ASCII or not (data and extended data).
     * <p/>
     * Will override SPT {@code Dc09Spt#dataCharsetToUtf8} when used.
     *
     * @param handleAllAsAscii whether to handle all as ASCII or not (data and extended data)
     *
     * @return the {@code Dc09GlobalParameters} instance
     */
    public Dc09GlobalParameters setHandleAllAsAscii(final boolean handleAllAsAscii) {
        this.handleAllAsAscii = handleAllAsAscii;
        return this;
    }

    /**
     * @return the minimum time difference between message and server
     */
    public int getMinDifferenceWithServer() {
        return minDifferenceWithServer;
    }

    /**
     * Sets the minimum time difference between message and server.
     *
     * @param minDifferenceWithServer the minimum time difference between message and server to set
     *
     * @return the {@code Dc09GlobalParameters} instance
     */
    public Dc09GlobalParameters setMinDifferenceWithServer(final int minDifferenceWithServer) {
        this.minDifferenceWithServer = minDifferenceWithServer;
        return this;
    }

    /**
     * @return the maximum time difference between message and server
     */
    public int getMaxDifferenceWithServer() {
        return maxDifferenceWithServer;
    }

    /**
     * Sets the maximum time difference between message and server.
     *
     * @param maxDifferenceWithServer the maximum time difference between message and server to set
     *
     * @return the {@code Dc09GlobalParameters} instance
     */
    public Dc09GlobalParameters setMaxDifferenceWithServer(final int maxDifferenceWithServer) {
        this.maxDifferenceWithServer = maxDifferenceWithServer;
        return this;
    }
}
