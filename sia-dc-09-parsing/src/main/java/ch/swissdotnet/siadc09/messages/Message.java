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
package ch.swissdotnet.siadc09.messages;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static ch.swissdotnet.siadc09.Dc09Utils.BASE_64;
import static ch.swissdotnet.siadc09.Dc09Utils.copy;
import static ch.swissdotnet.siadc09.messages.MessageType.*;
import static ch.swissdotnet.siadc09.messages.versions.Dc09Version.Version;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * The {@code Message} class represents all exchange between SPT and RCT with SIA DC-09 protocol.
 * <p/>
 * Messages can be ciphered, which is indicated with star (*) character in the ID field. The ciphering
 * algorithm used is AES CBC with a shared key of either 128, 192 or 256 bits. To obtain a valid block size,
 * ciphered messages are padded with variable random bytes.
 * <p/>
 * Two message type can be sent and received:
 * <ul>
 * <li>event message from the SPT to indicate an issue/restoral;</li>
 * <li>link supervision to indicate SPT presence, acknowledge or refuse messages</li>
 * </ul>
 * The event message format is {@code
 * <LF><CRC><0LLL><"ID"><seq><Rrcvr><Lpref><#acct>[<pad>|...data...][x...data...]<timestamp><CR>} where
 * <ul>
 * <li>{@code <LF>} is the line feed;</li>
 * <li>{@code <CRC>} is the CRC computed on message minus header (LF, CRC and length);</li>
 * <li>{@code <0LLL>} is the message length with zero character prefix;</li>
 * <li>{@code <"ID">} is the event payload type with cipher indication (star after first quote character);</li>
 * <li>{@code <seq>} is the sequence number from 0 to 9999</li>
 * <li>{@code <Rrcvr>} is the receiver number (optional) with R character prefix;</li>
 * <li>{@code <Lpref>} is the account prefix with L character prefix;</li>
 * <li>{@code <#acct>} is the account number with dash character prefix;</li>
 * <li>{@code [} character indicate the event data start;</li>
 * <li>{@code <pad>} is the padding in ciphered messages to align data at the right size for CBC cipher algorithm;</li>
 * <li>{@code ...data...} is the payload which type is defined by {@code <"ID">};</li>
 * <li>{@code ]} character indicate the event data end;</li>
 * <li>{@code [x...data...]} is the repeatable extended data payload identified by {@code x} character;</li>
 * <li>{@code <timestamp>} is the optionally present timestamp usually used in cipher method to prevent replaying messages;</li>
 * <li>{@code <CR>} is the carriage return.</li>
 * </ul>
 * The supervision messages are used to supervise connection between SPT and RCT, and also to either acknowledge
 * or reject event messages. Their form is similar to event messages with their {@code <"ID">} part being fixed.
 * The <i>SHOULD</i> not contains additional data but some manufacturer embeds them regardless. Their IDs are:
 * <ul>
 * <li>{@code NULL} is the polling message from SPT to RCT to indicate its presence;</li>
 * <li>{@code ACK} is the acknowledgment message from RCT to SPT to indicate message acceptance;</li>
 * <li>{@code NAK} is the negative acknowledgment message from RCT to SPT to indicate message rejection due to
 * timestamp check fail;</li>
 * <li>{@code DUH} is the unable to acknowledge message from RCT to SPT to indicate message unable to be delivered.</li>
 * </ul>
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public abstract class Message {

    // Message version.
    private final Version version;
    // The CRC read.
    private final byte[] readCrc;
    // The CRC expected on given read message.
    private final byte[] expectedCrc;
    // The length (<0LLL>) read.
    private final int readLength;
    // The length actually read.
    private final int actualLength;
    // Whether the message is ciphered or not.
    private final boolean ciphered;
    // The message sequence.
    private final int sequence;
    // The optionally present receiver number.
    private final Optional<String> receiverNumber;
    // The account number prefix.
    private final String accountPrefix;
    // The account number.
    private final String accountNumber;
    // The account number present in data.
    private final String accountNumberData;
    // The optionally present timestamp.
    private final Optional<DateTime> timestamp;
    // Message padding.
    private final Optional<byte[]> padding;
    // The sent message.
    private final byte[] message;
    // The sent payload.
    private final byte[] payload;

    Message(final Builder builder) {
        this.version = builder.version;
        this.padding = builder.optPadding;
        this.ciphered = builder.ciphered;
        this.readCrc = builder.readCrc;
        this.expectedCrc = builder.expectedCrc;
        this.readLength = builder.readLength;
        this.actualLength = builder.actualLength;
        this.sequence = builder.sequence;
        this.receiverNumber = builder.optReceiverNumber;
        this.accountPrefix = builder.accountPrefix;
        this.accountNumber = builder.accountNumber;
        this.accountNumberData = builder.accountNumberData;
        this.timestamp = builder.optTimestamp;
        this.message = builder.message;
        this.payload = builder.payload;
    }

    /**
     * Creates a new message {@code Builder} with given type.
     *
     * @param type the builder to create with given type
     *
     * @return the newly created message builder
     */
    public static Builder newBuilder(final MessageType type) {
        return new Builder()
            .type(type);
    }

    /**
     * Creates a new acknowledge message {@code Builder}.
     *
     * @return the {@code Builder} with acknowledge type
     */
    public static Builder newAckBuilder() {
        return new Builder()
            .type(ACK);
    }

    /**
     * Creates a new negative acknowledge message {@code Builder}.
     *
     * @return the {@code Builder} with negative acknowledge type
     */
    public static Builder newNakBuilder() {
        return new Builder()
            .type(NAK)
            .sequence(0)
            .receiverNumber("0")
            .accountPrefix("0")
            .accountNumber("0");
    }

    /**
     * Creates a new polling message {@code Builder}.
     *
     * @return the {@code Builder} with polling type
     */
    public static Builder newPollingBuilder() {
        return new Builder()
            .type(NULL)
            .sequence(0);
    }

    /**
     * Creates a new event message {@code Builder} with given type and data.
     *
     * @param id   the message payload type
     * @param data the data associated with event
     *
     * @return the {@code Builder} with given payload type and data
     */
    public static Builder newEventBuilder(final String id, final String data) {
        return new Builder()
            .type(EVENT)
            .id(id)
            .data(data);
    }

    /**
     * Creates a new message response {@code Builder} with given data.
     *
     * @param data the data associated with response message
     *
     * @return the {@code Builder} with given data
     */
    public static Builder newRspBuilder(final String data) {
        return new Builder()
            .type(RSP)
            .data(data);
    }

    /**
     * Creates a new unable acknowledgement message {@code Builder}.
     *
     * @return the {@code Builder} with unable acknowledgement type
     */
    public static Builder newDuhBuilder() {
        return new Builder()
            .type(DUH);
    }

    /**
     * Creates a new message {@code Builder} with given message content.
     *
     * @param message the message to create builder from
     *
     * @return the {@code Builder} with given message content
     */
    public static Builder newBuilder(final Message message) {
        Builder builder = new Builder()
            .readCrc(message.readCrc)
            .actualCrc(message.expectedCrc)
            .readLength(message.readLength)
            .actualLength(message.actualLength)
            .version(message.version)
            .ciphered(message.ciphered)
            .sequence(message.sequence)
            .timestamp(message.timestamp)
            .receiverNumber(message.receiverNumber)
            .accountPrefix(message.accountPrefix)
            .accountNumber(message.accountNumber)
            .accountNumberData(message.accountNumberData)
            .type(message.type())
            .id(message.getId())
            .message(message.message)
            .payload(message.payload);

        message.padding.ifPresent(builder::padding);

        if (message instanceof DataMessage) {
            DataMessage dataMessage = (DataMessage) message;
            builder.data(dataMessage.getData())
                .additionalData(dataMessage.getAdditional());
        }

        return builder;
    }

    /**
     * @return the SIA DC-09 version
     *
     * @see ch.swissdotnet.siadc09.messages.versions.Dc09Version
     */
    public Version getVersion() {
        return version;
    }

    /**
     * @return the message ID (i.e. ACK, SIA-DSC, ...)
     */
    public String getId() {
        return type().toString();
    }

    /**
     * @return whether the message is/should be ciphered or not
     */
    public boolean isCiphered() {
        return ciphered;
    }

    /**
     * @return the read CRC from message
     */
    public byte[] getReadCrc() {
        return copy(readCrc);
    }

    /**
     * @return the excepted CRC from message
     */
    public byte[] getExpectedCrc() {
        return copy(expectedCrc);
    }

    /**
     * @return the read length from message
     */
    public int getReadLength() {
        return readLength;
    }

    /**
     * @return the actual length from message
     */
    public int getActualLength() {
        return actualLength;
    }

    /**
     * @return the message sequence
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * @return the optionally present receiver number
     */
    public Optional<String> getReceiverNumber() {
        return receiverNumber;
    }

    /**
     * @return the account prefix
     */
    public String getAccountPrefix() {
        return accountPrefix;
    }

    /**
     * @return the account number
     */
    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * @return the account number in data portion (which may deviate from account number)
     */
    public String getAccountNumberData() {
        return accountNumberData;
    }

    /**
     * @return the optionally present timestamp
     */
    public Optional<DateTime> getTimestamp() {
        return timestamp;
    }

    /**
     * @return the message which encompass the whole read message
     */
    public byte[] getMessage() {
        return copy(message);
    }

    /**
     * @return the optionally present padding
     */
    public Optional<byte[]> getPadding() {
        return padding;
    }

    /**
     * @return the optionally present payload
     */
    public Optional<byte[]> getPayload() {
        return ofNullable(payload);
    }

    /**
     * @return whether a read message is valid by having the right length and the right CRC
     */
    public boolean isValid() {
        // Message is valid if:
        // - Sizes are the same,
        // - CRCs are the same.Dc09Utils.BASE_64.encode(readCrc)
        String readCrcStr = BASE_64.encode(readCrc);
        String expectedCrcStr = BASE_64.encode(expectedCrc);
        return (actualLength == readLength)
            && readCrcStr.equalsIgnoreCase(expectedCrcStr);
        //&& Arrays.equals(readCrc, expectedCrc);
    }

    /**
     * @return the message type
     */
    abstract public MessageType type();

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Message.class)
            .omitNullValues()
            .add("type", type())
            .add("valid", isValid())
            .add("padding", padding.isPresent() ? BASE_64.encode(padding.get()) : "empty")
            .add("ciphered", isCiphered())
            .add("readCrc", readCrc != null ? BASE_64.encode(readCrc) : "empty")
            .add("expectedCrc", expectedCrc != null ? BASE_64.encode(expectedCrc) : "empty")
            .add("readLength", readLength)
            .add("actualLength", actualLength)
            .add("sequence", getSequence())
            .add("receiverNumber", getReceiverNumber())
            .add("accountPrefix", getAccountPrefix())
            .add("accountNumber", getAccountNumber())
            .add("accountNumberData", getAccountNumberData())
            .add("timestamp", getTimestamp())
            .add("payload", payload != null ? BASE_64.encode(payload) : "empty")
            .add("message", message != null ? BASE_64.encode(message) : "empty")
            .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        final Message otherMessage = (Message) o;
        return readLength == otherMessage.readLength
            && actualLength == otherMessage.actualLength
            && ciphered == otherMessage.ciphered
            && sequence == otherMessage.sequence
            && version == otherMessage.version
            && Arrays.equals(readCrc, otherMessage.readCrc)
            && Arrays.equals(expectedCrc, otherMessage.expectedCrc)
            && Objects.equals(receiverNumber, otherMessage.receiverNumber)
            && Objects.equals(accountPrefix, otherMessage.accountPrefix)
            && Objects.equals(accountNumber, otherMessage.accountNumber)
            && Objects.equals(accountNumberData, otherMessage.accountNumberData)
            && Objects.equals(timestamp, otherMessage.timestamp)
            && Objects.equals(padding, otherMessage.padding)
            && Arrays.equals(message, otherMessage.message)
            && Arrays.equals(payload, otherMessage.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
            version,
            readLength,
            actualLength,
            ciphered,
            sequence,
            receiverNumber,
            accountPrefix,
            accountNumber,
            accountNumberData,
            timestamp,
            padding
        );
        result = 31 * result + Arrays.hashCode(readCrc);
        result = 31 * result + Arrays.hashCode(expectedCrc);
        result = 31 * result + Arrays.hashCode(message);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    /**
     * The {@code Builder} class follows the builder pattern to creates new {@code Message}.
     * <p/>
     * It is not thread safe and should be used in conjunction with static factories.
     */
    public static final class Builder {

        // Mutable additional data.
        final List<AdditionalData> additional = Lists.newArrayList();
        // Mutable MAC address.
        String mac = null;
        // Mutable data.
        String data = "";
        // Mutable message ID.
        String id = "";
        // Mutable SIA DC-09 version.
        private Version version = Version.STANDARD;
        // Mutable message type.
        private MessageType type;
        // Mutable timestamp.
        private Optional<DateTime> optTimestamp = empty();
        // Mutable receiver number.
        private Optional<String> optReceiverNumber = empty();
        // Mutable sequence.
        private int sequence = -1;
        // Mutable read crc.
        private byte[] readCrc = new byte[0];
        // Mutable expected crc.
        private byte[] expectedCrc = new byte[0];
        // Mutable read length.
        private int readLength;
        // Mutable actual length.
        private int actualLength;
        // Mutable account prefix.
        private String accountPrefix = "0";
        // Mutable account number.
        private String accountNumber = "";
        // Mutable account data number.
        private String accountNumberData = "";
        // Mutable padding.
        private Optional<byte[]> optPadding = empty();
        // Mutable message content.
        private byte[] message = new byte[0];
        // Mutable payload.
        private byte[] payload = new byte[0];
        // Mutable cipher flag.
        private boolean ciphered = false;

        /**
         * Sets the read CRC value.
         *
         * @param readCrc the read CRC value to set
         *
         * @return the {@code Builder} instance
         */
        public Builder readCrc(final byte[] readCrc) {
            this.readCrc = copy(readCrc);
            return this;
        }

        /**
         * Sets the actual CRC value.
         *
         * @param expectedCrc the expected CRC value to set
         *
         * @return the {@code Builder} instance
         */
        public Builder actualCrc(final byte[] expectedCrc) {
            this.expectedCrc = copy(expectedCrc);
            return this;
        }

        /**
         * Sets the length read.
         *
         * @param readLength the length read to set
         *
         * @return the {@code Builder} instance
         */
        public Builder readLength(final int readLength) {
            this.readLength = readLength;
            return this;
        }

        /**
         * Sets the actual length.
         *
         * @param actualLength the actual length to set
         *
         * @return the {@code Builder} instance
         */
        public Builder actualLength(final int actualLength) {
            this.actualLength = actualLength;
            return this;
        }

        /**
         * Sets the SIA DC-09 version.
         *
         * @param version the SIA DC-09 version used by the message
         *
         * @return the {@code Builder} instance
         */
        public Builder version(final Version version) {
            this.version = version;
            return this;
        }

        /**
         * Sets whether the message is/should be ciphered.
         *
         * @param ciphered whether the message is/should be ciphered or not
         *
         * @return the {@code Builder} instance
         */
        public Builder ciphered(final boolean ciphered) {
            this.ciphered = ciphered;
            return this;
        }

        /**
         * Sets the message sequence number.
         *
         * @param sequence the sequence number to set
         *
         * @return the {@code Builder} instance
         */
        public Builder sequence(final int sequence) {
            this.sequence = sequence;
            return this;
        }

        /**
         * Sets the timestamp.
         *
         * @param timestamp the timestamp to set
         *
         * @return the {@code Builder} instance
         */
        public Builder timestamp(final DateTime timestamp) {
            this.optTimestamp = ofNullable(timestamp);
            return this;
        }

        /**
         * Sets the optionally present timestamp.
         *
         * @param optTimestamp the optionally present timestamp to set
         *
         * @return the {@code Builder} instance
         */
        public Builder timestamp(final Optional<DateTime> optTimestamp) {
            this.optTimestamp = optTimestamp;
            return this;
        }

        /**
         * Sets the receiver number.
         *
         * @param receiverNumber the receiver number to set
         *
         * @return the {@code Builder} instance
         */
        public Builder receiverNumber(final String receiverNumber) {
            this.optReceiverNumber = ofNullable(receiverNumber);
            return this;
        }

        /**
         * Sets the optionally present receiver number.
         *
         * @param optReceiverNumber the optionally present receiver number to set
         *
         * @return the {@code Builder} instance
         */
        public Builder receiverNumber(final Optional<String> optReceiverNumber) {
            this.optReceiverNumber = optReceiverNumber;
            return this;
        }

        /**
         * Sets the account prefix.
         *
         * @param accountPrefix the account prefix to set
         *
         * @return the {@code Builder} instance
         */
        public Builder accountPrefix(final String accountPrefix) {
            this.accountPrefix = accountPrefix;
            return this;
        }

        /**
         * Sets the account number.
         *
         * @param accountNumber the account number to set
         *
         * @return the {@code Builder} instance
         */
        public Builder accountNumber(final String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        /**
         * Sets the message padding
         *
         * @param padding the message padding to set
         *
         * @return the {@code Builder} instance
         */
        public Builder padding(final byte[] padding) {
            this.optPadding = ofNullable(copy(padding));
            return this;
        }

        /**
         * Sets the message type.
         *
         * @param type the message type to set
         *
         * @return the {@code Builder} instance
         */
        public Builder type(final MessageType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the message data identifier (i.e. SIA-DSC, ...)
         *
         * @param id the message data identifier to set
         *
         * @return the {@code Builder} instance
         */
        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the data account number.
         *
         * @param accountNumberData the data account number to set
         *
         * @return the {@code Builder} instance
         */
        public Builder accountNumberData(final String accountNumberData) {
            this.accountNumberData = accountNumberData;
            return this;
        }

        /**
         * Sets the message data.
         *
         * @param data the message data to set
         *
         * @return the {@code Builder} instance
         */
        public Builder data(final String data) {
            this.data = data;
            return this;
        }

        /**
         * Sets the additional data.
         *
         * @param additional the additional data to set
         *
         * @return the {@code Builder} instance
         */
        public Builder additionalData(final List<AdditionalData> additional) {
            this.additional.clear();
            this.additional.addAll(additional);
            return this;
        }

        /**
         * Sets the MAC address.
         *
         * @param additional the additional data to set
         *
         * @return the {@code Builder} instance
         */
        public Builder macAddress(final String additional) {
            this.mac = additional;
            return this;
        }

        /**
         * Sets the whole message in byte array.
         *
         * @param message the message to set
         *
         * @return the {@code Builder} instance
         */
        public Builder message(final byte[] message) {
            this.message = copy(message);
            return this;
        }

        /**
         * Sets the message payload in byte array.
         *
         * @param payload the message payload to set
         *
         * @return the {@code Builder} instance
         */
        public Builder payload(final byte[] payload) {
            this.payload = copy(payload);
            return this;
        }

        /**
         * Builds the appropriate message type based on specified type.
         *
         * @return a newly create {@code Message} with parameters from {@code Builder}
         */
        public Message build() {

            switch (type) {
                case ACK:
                    return new Ack(this);
                case NAK:
                    return new Nak(this);
                case DUH:
                    return new Duh(this);
                case NULL:
                    return new Polling(this);
                case RSP:
                    return new Rsp(this);
                default:
                case EVENT:
                    return new Event(this);
            }

        }
    }

}
