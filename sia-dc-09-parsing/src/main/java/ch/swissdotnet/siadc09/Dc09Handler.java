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

import ch.swissdotnet.siadc09.exceptions.InvalidCipheringException;
import ch.swissdotnet.siadc09.exceptions.InvalidMessageException;
import ch.swissdotnet.siadc09.messages.AdditionalData;
import ch.swissdotnet.siadc09.messages.DataMessage;
import ch.swissdotnet.siadc09.messages.Message;
import ch.swissdotnet.siadc09.messages.MessageType;
import ch.swissdotnet.siadc09.messages.versions.BinaryCrcDc09;
import ch.swissdotnet.siadc09.messages.versions.Dc09Version;
import ch.swissdotnet.siadc09.messages.versions.StandardDc09;
import ch.swissdotnet.siadc09.parameters.Dc09GlobalParameters;
import ch.swissdotnet.siadc09.parameters.Dc09Spt;
import ch.swissdotnet.siadc09.parameters.Dc09SptParameters;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static ch.swissdotnet.siadc09.Dc09Utils.*;
import static ch.swissdotnet.siadc09.messages.AdditionalData.AdditionalDataType;
import static ch.swissdotnet.siadc09.messages.AdditionalData.AdditionalDataType.*;
import static ch.swissdotnet.siadc09.messages.versions.Dc09Version.Version.BINARY_CRC;
import static ch.swissdotnet.siadc09.messages.versions.Dc09Version.Version.STANDARD;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.*;

/**
 * The {@code Dc09Handler} implements ANSI/SIA DC-09-2013 message parsing/serializing.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public class Dc09Handler implements Dc09Reader, Dc09Writer {

    // Absent account number.
    private static final Optional<String> ABSENT_ACCOUNT_NUMBER = empty();

    public static Dc09Version.Version detectDc09Version(final byte[] crcDetection) {

        Dc09Version.Version version = STANDARD;
        int index = Bytes.indexOf(crcDetection, QUOTE);
        if (index <= 6) {
            version = BINARY_CRC;
        }
        return version;
    }

    public static Dc09Version buildVersion(final Dc09Version.Version dc09Version) {
        Dc09Version version = new StandardDc09();
        switch (dc09Version) {
            case STANDARD:
                break;
            case BINARY_CRC:
                version = new BinaryCrcDc09();
                break;
        }
        return version;
    }

    @Override
    public Message read(final byte[] data, final Dc09GlobalParameters global, final Dc09SptStore store) throws InvalidMessageException {

        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        // <LF> Line feed.
        incrementAttestingValue(bais, LF, ABSENT_ACCOUNT_NUMBER);

        // Peek the next 4 bytes to detect which CRC we are dealing with.
        byte[] crcDetection = peek(bais, MAXIMUM_PEEK_QUOTE, ABSENT_ACCOUNT_NUMBER);
        Dc09Version.Version dc09Version = detectDc09Version(crcDetection);
        Dc09Version version = buildVersion(dc09Version);

        // <crc> The data CRC.
        byte[] readCrc = readBytes(bais, version.crcSize(), ABSENT_ACCOUNT_NUMBER);

        // <0LLL>
        incrementAttestingValue(bais, ZERO, ABSENT_ACCOUNT_NUMBER);
        byte[] lengthByte = readBytes(bais, version.lengthSize(), ABSENT_ACCOUNT_NUMBER);
        int readLength = hexBytesToInt(lengthByte);

        // <"id"> ID Token.
        incrementAttestingValue(bais, QUOTE, ABSENT_ACCOUNT_NUMBER);
        ByteArrayDataOutput middle = ByteStreams.newDataOutput();
        middle.write(QUOTE);
        boolean ciphered = peek(bais) == STAR;
        // Skip the * if the message is ciphered.
        if (ciphered) {
            incrementAttestingValue(bais, STAR, ABSENT_ACCOUNT_NUMBER);
            middle.write(STAR);
        }
        byte[] id = readUntil(bais, QUOTE, readLength, ABSENT_ACCOUNT_NUMBER);
        middle.write(id);
        incrementAttestingValue(bais, QUOTE, ABSENT_ACCOUNT_NUMBER);
        middle.write(QUOTE);

        // Creates messages with ID.
        MessageType type = MessageType.fromByte(id);
        Message.Builder builder = Message.newBuilder(type)
            .readCrc(readCrc)
            .readLength(readLength)
            .id(new String(id, StandardCharsets.UTF_8))
            .ciphered(ciphered)
            .version(dc09Version)
            .type(type);

        Optional<Dc09Spt> optSpt = empty();
        Optional<DateTime> optTimestamp;
        Optional<String> optAccountNumber;
        String accountNumber;

        // When dealing with NAK messages, ensures its content is "0000R0L0A0[]".
        if (type == MessageType.NAK) {

            // <seq> Sequence number.
            int sequence = readSequence(bais, middle, version);
            builder.sequence(sequence);
            int dashIndexOf = indexOf(bais, DASH, NAK_STATIC_DATA_LENGTH);
            // <Rrcvr> Receiver number.
            Optional<String> optReceiverNumber = readReceiverNumber(bais, middle, readLength, version);
            builder.receiverNumber(optReceiverNumber);
            byte stopChar = -1 == dashIndexOf ? A : DASH;
            // <Lpref> Account prefix.
            String accountPrefix = readAccountPrefix(bais, middle, stopChar, readLength, version);
            builder.accountPrefix(accountPrefix);
            // <#acct> Account number.
            accountNumber = readAccountNumber(bais, middle, stopChar, readLength, version);
            builder.accountNumber(accountNumber);
            optAccountNumber = Optional.of(accountNumber);

            // Beginning of data.
            incrementAttestingValue(bais, BRACKET_OPEN, optAccountNumber);
            middle.write(BRACKET_OPEN);
            incrementAttestingValue(bais, BRACKET_CLOSE, optAccountNumber);
            middle.write(BRACKET_CLOSE);

            optTimestamp = readTimestamp(bais, accountNumber, version);
            if (optTimestamp.isPresent()) {
                middle.write(UNDERSCORE);
                byte[] date = version.timestampFormat().print(optTimestamp.get()).getBytes(StandardCharsets.UTF_8);
                middle.write(date);
            }
            builder.timestamp(optTimestamp);

        } else {

            // <seq> Sequence number.
            int sequence = readSequence(bais, middle, version);
            builder.sequence(sequence);
            // <Rrcvr> Receiver number.
            Optional<String> optReceiverNumber = readReceiverNumber(bais, middle, readLength, version);
            builder.receiverNumber(optReceiverNumber);
            // <Lpref> Account prefix.
            String accountPrefix = readAccountPrefix(bais, middle, DASH, readLength, version);
            builder.accountPrefix(accountPrefix);
            // <#acct> Account number.
            accountNumber = readAccountNumber(bais, middle, DASH, readLength, version);
            builder.accountNumber(accountNumber);
            optAccountNumber = of(accountNumber);
            // Beginning of data.
            incrementAttestingValue(bais, BRACKET_OPEN, optAccountNumber);
            middle.write(BRACKET_OPEN);

            byte[] payload = readUntil(bais, CR, readLength, optAccountNumber);
            middle.write(payload);
            ByteArrayInputStream payloadBais = new ByteArrayInputStream(payload);

            byte finisher = PIPE;

            optSpt = store.findSpt(optReceiverNumber, accountPrefix, accountNumber);

            // When messages is ciphered:
            // - tries to lookup SPT with ciphering algorithm,
            // - decipher content,
            // - check which finisher is used to separate data from padding
            //   (as DC09 norm shows valid NULL and ACK messages without any separator,
            //    and some implementation also add additional data to NULL messages),
            // - stores padding content.
            if (ciphered) {
                if (optSpt.isEmpty()) {
                    throw new InvalidMessageException(optAccountNumber, "Invalid message: unable to find SPT " + accountNumber + " in store.");
                }
                Dc09Spt spt = optSpt.get();
                Dc09SptParameters parameters = spt.getParameters();
                if (!parameters.usesCiphering()) {
                    throw new InvalidMessageException(optAccountNumber, "Invalid message: found SPT without any cipher algorithm.");
                }

                String payloadString = asString(payload);
                try {
                    payload = parameters.cipherAlgorithm().decipher(hexStringToBytes(payloadString));
                } catch (final InvalidCipheringException e) {
                    throw new InvalidMessageException(optAccountNumber, e);
                }

                payloadBais = new ByteArrayInputStream(payload);
                int indexOfPipe = indexOf(payloadBais, PIPE, payload.length);
                if (indexOfPipe < 0) {
                    finisher = BRACKET_CLOSE;
                }

                byte[] padding = readUntil(payloadBais, finisher, payload.length, optAccountNumber);
                builder.padding(padding);
                incrementAttestingValue(payloadBais, finisher, optAccountNumber);
            }

            // Gathers data and additional data.
            if (type.containsData()) {
                builder.accountNumberData(readAccountNumberData(payloadBais, optAccountNumber, readLength, version, PIPE));
                incrementAttestingValue(payloadBais, PIPE, optAccountNumber);
                builder.data(readData(payloadBais, optAccountNumber, optSpt, readLength, global.isHandleAllAsAscii()));
                incrementAttestingValue(payloadBais, BRACKET_CLOSE, optAccountNumber);
                AdditionalDataHolder additionalDataHolder = readExtendedData(
                    payloadBais,
                    optAccountNumber,
                    optSpt,
                    readLength,
                    global.isHandleAllAsAscii()
                );
                builder.additionalData(additionalDataHolder.additionalData);
                builder.macAddress(additionalDataHolder.mac);
            } else {
                if (finisher != BRACKET_CLOSE) {
                    int indexOfBracketClose = indexOf(payloadBais, BRACKET_CLOSE, payload.length);
                    if (indexOfBracketClose > 0) {
                        builder.accountNumberData(readAccountNumberData(payloadBais, optAccountNumber, readLength, version, BRACKET_CLOSE));
                    }
                    incrementAttestingValue(payloadBais, BRACKET_CLOSE, optAccountNumber);
                }
                AdditionalDataHolder additionalDataHolder = readExtendedData(
                    payloadBais,
                    optAccountNumber,
                    optSpt,
                    readLength,
                    global.isHandleAllAsAscii()
                );
                builder.additionalData(additionalDataHolder.additionalData);
                builder.macAddress(additionalDataHolder.mac);
            }
            builder.payload(payload);

            optTimestamp = readTimestamp(payloadBais, accountNumber, version);
            builder.timestamp(optTimestamp);
        }

        byte[] middleByte = middle.toByteArray();
        builder.readLength(readLength);
        builder.actualLength(middleByte.length);

        incrementAttestingValue(bais, CR, optAccountNumber);

        // Checks the message constraints about ciphering.
        if (ciphered) {
            if (!type.mayBeCiphered()) {
                throw new InvalidMessageException(optAccountNumber, "Invalid message: NAK and DUH should never be ciphered.");
            }
        } else {
            if (optSpt.isPresent() && optSpt.get().getParameters().usesCiphering() && (type != MessageType.NAK && type != MessageType.DUH)) {
                throw new InvalidMessageException(optAccountNumber, "Invalid message: message is not ciphered but should be.");
            }
        }

        byte[] actualCrc = version.crc(middleByte, global.isHexToUpper());
        builder.actualCrc(actualCrc);

        builder.message(data);

        return builder.build();

    }

    @Override
    public SerializedMessage write(final Message message, final Dc09GlobalParameters global, final Dc09SptParameters spt)
        throws InvalidMessageException {

        boolean ciphered = spt.usesCiphering();

        Dc09Version version = buildVersion(message.getVersion());

        ByteArrayDataOutput middle = ByteStreams.newDataOutput();

        // <"id"> ID token.
        middle.write(writeToken(message, ciphered));

        // <"seq">.
        writeSequence(message, version, middle);

        // <Rrcvr> Receiver Number.
        writeReceiverNumber(message, global, middle);

        // <Lpref> Account Prefix.
        writeAccountPrefix(message, global, middle);

        // <#acct> Account Number.
        writeAccountNumber(message, global, middle);

        // Data begin.
        middle.write(BRACKET_OPEN);

        // Data.
        ByteArrayDataOutput data = ByteStreams.newDataOutput();
        try {
            if (message.type().containsData()) {

                if (ciphered) {
                    data.write(PIPE);
                }

                DataMessage dataMessage = (DataMessage) message;
                writeAccountDataNumber(dataMessage, global, data);

                data.write(PIPE);
                data.write(dataMessage.getData().getBytes(StandardCharsets.UTF_8));
                data.write(BRACKET_CLOSE);

            } else {
                if (ciphered && message.type() == MessageType.ACK && global.isAppendPipeToAck()) {
                    data.write(PIPE);
                } else if (ciphered && message.type() == MessageType.NULL && global.isAppendPipeToPolling()) {
                    data.write(PIPE);
                }
                data.write(BRACKET_CLOSE);
            }

            if (message.type().mayContainAdditionalData()) {

                DataMessage dataMessage = (DataMessage) message;
                for (AdditionalData additionalData : dataMessage.getAdditional()) {
                    data.write(BRACKET_OPEN);
                    if (additionalData.getType() != UNKNOWN) {
                        data.write(additionalData.getType().getIdentifier());
                    }
                    data.write(additionalData.getContent().getBytes(StandardCharsets.UTF_8));
                    data.write(BRACKET_CLOSE);
                }

            }

        } catch (final ClassCastException e) {
            throw new InvalidMessageException(message.getAccountNumber(), "Invalid message: unable to get data from [" + message.type() + "].");
        }

        // Timestamp (optional).
        Optional<DateTime> optTimestamp = message.getTimestamp();
        if (optTimestamp.isPresent()) {
            data.write(UNDERSCORE);
            data.write(version.timestampFormat().print(optTimestamp.get()).getBytes(StandardCharsets.UTF_8));
        }

        byte[] dataBytes = data.toByteArray();
        try {
            if (ciphered && message.type().mayBeCiphered()) {
                dataBytes = spt.cipherAlgorithm().cipher(dataBytes, version);
                BaseEncoding baseEncoding = BaseEncoding.base16();
                if (!global.isHexToUpper()) {
                    baseEncoding = baseEncoding.lowerCase();
                }
                String encoded = baseEncoding.encode(dataBytes);
                dataBytes = encoded.getBytes(StandardCharsets.UTF_8);
            }
        } catch (InvalidCipheringException e) {
            throw new InvalidMessageException(message.getAccountNumber(), "Invalid message: unable to cipher message: " + e.getMessage() + "");
        }

        // Data end.
        middle.write(dataBytes);

        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        byte[] middleBytes = middle.toByteArray();

        try {
            // <LF> Linefeed.
            serialized.write(LF);
            // <crc> CRC.
            byte[] crc = version.crc(middleBytes, global.isHexToUpper());
            serialized.write(crc);
            // <0LLL> Empty length for replacement later on.
            byte[] length = writeMessageLength(middleBytes.length, version);
            serialized.write(length);
            // Middle message.
            serialized.write(middleBytes);
            // <CR> Carriage return.
            serialized.write(CR);
        } catch (final IOException e) {
            throw new InvalidMessageException(
                message.getAccountNumber(),
                "Invalid message: unable to write serialized message (" + e.getMessage() + ")."
            );
        }

        return new SerializedMessage(serialized.toByteArray(), data.toByteArray());

    }

    private Optional<DateTime> readTimestamp(final ByteArrayInputStream bais, final String accountNumber, final Dc09Version version)
        throws InvalidMessageException {

        byte underscore = peek(bais);
        Optional<DateTime> response = empty();
        if (underscore == UNDERSCORE) {
            incrementAttestingValue(bais, UNDERSCORE, ofNullable(accountNumber));
            byte[] timestamp = readBytes(bais, version.timestampSize(), ABSENT_ACCOUNT_NUMBER);
            String timestampString = asString(timestamp);
            try {
                DateTime current = version.timestampFormat().parseDateTime(timestampString);
                response = of(new LocalDateTime(current).toDateTime(DateTimeZone.UTC));
            } catch (final IllegalArgumentException e) {
                throw new InvalidMessageException("Invalid message: bad timestamp format [" + timestampString + "].");
            }
        }
        return response;

    }

    private int readSequence(final ByteArrayInputStream bais, final ByteArrayDataOutput middle, final Dc09Version version)
        throws InvalidMessageException {

        byte[] size = readBytes(bais, version.seqSize(), ABSENT_ACCOUNT_NUMBER);
        String asciiValue = bytesToAsciiString(size);
        try {
            int sequence = Integer.parseInt(asciiValue);
            if (sequence >= 0 && sequence <= version.maxSequenceValue()) {
                middle.write(size);
                return sequence;
            } else {
                throw new InvalidMessageException("Invalid message: impossible sequence number [" + sequence + "].");
            }
        } catch (final NumberFormatException e) {
            throw new InvalidMessageException("Invalid message: bad sequence number format [" + asciiValue + "].");
        }

    }

    private Optional<String> readReceiverNumber(final ByteArrayInputStream bais,
                                                final ByteArrayDataOutput middle,
                                                final int max,
                                                final Dc09Version version) throws InvalidMessageException {

        byte peek = peek(bais);
        Optional<String> response = empty();
        // Only check when a receiver number is transmitted.
        if (peek == R) {
            // Ignored R.
            readByte(bais, ABSENT_ACCOUNT_NUMBER);
            byte[] receiverNumber = readUntil(bais, L, max, ABSENT_ACCOUNT_NUMBER);
            int length = receiverNumber.length;
            if (length > version.maxRcvrSize() || length == 0) {
                throw new InvalidMessageException("Invalid message: the receiver number size is wrong [" + length + "].");
            }
            response = of(bytesToAsciiString(receiverNumber));
            middle.write(R);
            middle.write(receiverNumber);
        }
        return response;

    }

    private String readAccountPrefix(final ByteArrayInputStream bais,
                                     final ByteArrayDataOutput middle,
                                     final byte stopChar,
                                     final int max,
                                     final Dc09Version version) throws InvalidMessageException {

        incrementAttestingValue(bais, L, ABSENT_ACCOUNT_NUMBER);
        byte[] accountPrefix = readUntil(bais, stopChar, max, ABSENT_ACCOUNT_NUMBER);
        int length = accountPrefix.length;
        if (length > version.maxPrefSize() || length == 0) {
            throw new InvalidMessageException("Invalid message: the account prefix size is wrong [" + length + "].");
        }
        middle.write(L);
        middle.write(accountPrefix);

        return bytesToAsciiString(accountPrefix);

    }

    private String readAccountNumber(final ByteArrayInputStream bais,
                                     final ByteArrayDataOutput middle,
                                     final byte startChar,
                                     final int max,
                                     final Dc09Version version) throws InvalidMessageException {

        incrementAttestingValue(bais, startChar, ABSENT_ACCOUNT_NUMBER);
        byte[] accountNumber = readUntil(bais, BRACKET_OPEN, max, ABSENT_ACCOUNT_NUMBER);
        int length = accountNumber.length;
        if (length > version.maxAccSize() || length == 0) {
            throw new InvalidMessageException("Invalid message: the account number size is wrong [" + length + "].");
        }
        middle.write(startChar);
        middle.write(accountNumber);

        return bytesToAsciiString(accountNumber);

    }

    private String readAccountNumberData(final ByteArrayInputStream bais,
                                         final Optional<String> accountNumber,
                                         final int max,
                                         final Dc09Version version,
                                         final byte readUntil) throws InvalidMessageException {

        int dash = indexOf(bais, DASH, version.maxAccSize());
        if (dash >= 0) {
            incrementAttestingValue(bais, DASH, ABSENT_ACCOUNT_NUMBER);
        }
        byte[] dataAccountNumber = readUntil(bais, readUntil, max, accountNumber);
        int length = dataAccountNumber.length;
        if (length > version.maxAccSize() || length == 0) {
            throw new InvalidMessageException(accountNumber, "Invalid message: the data account number size is wrong [" + length + "].");
        }

        return bytesToAsciiString(dataAccountNumber);

    }

    private String readData(final ByteArrayInputStream bais,
                            final Optional<String> accountNumber,
                            final Optional<Dc09Spt> optSpt,
                            final int max,
                            final boolean handleAllAsAscii) throws InvalidMessageException {
        byte[] data = readUntil(bais, BRACKET_CLOSE, max, accountNumber);
        if (optSpt.isPresent() && optSpt.get().isDataCharsetToUtf8() && !handleAllAsAscii) {
            return new String(data, UTF_8);
        } else {
            return bytesToAsciiString(data);
        }
    }

    private AdditionalDataHolder readExtendedData(final ByteArrayInputStream bais,
                                                  final Optional<String> accountNumber,
                                                  final Optional<Dc09Spt> optSpt,
                                                  final int max,
                                                  final boolean handleAllAsAscii) throws InvalidMessageException {

        byte peek = peek(bais);
        List<AdditionalData> response = Lists.newArrayList();
        String mac = null;
        // Only check when a opening bracket is transmitted.
        do {
            if (peek == BRACKET_OPEN) {
                incrementAttestingValue(bais, BRACKET_OPEN, accountNumber);
                byte[] extendedData = readUntil(bais, BRACKET_CLOSE, max, accountNumber);
                if (extendedData != null && extendedData.length > 0) {
                    AdditionalDataType type = forIdentifier((char) extendedData[0]);
                    String payload;
                    if (handleAllAsAscii) {
                        payload = bytesToAsciiString(extendedData);
                    } else {
                        Charset charset = (optSpt.isPresent() && optSpt.get().isDataCharsetToUtf8()) ? UTF_8 : type.getCharset();
                        payload = new String(extendedData, charset);
                    }
                    if (!payload.isEmpty()) {
                        String additionalData = payload.substring(type == UNKNOWN ? 0 : 1);
                        response.add(new AdditionalData(type, additionalData));
                        if (type == MAC_ADDRESS) {
                            mac = additionalData;
                        }
                    }
                }
                incrementAttestingValue(bais, BRACKET_CLOSE, accountNumber);
            }
            peek = peek(bais);
        } while (peek == BRACKET_OPEN);
        return new AdditionalDataHolder(response, mac);

    }

    private byte[] writeMessageLength(final int length, final Dc09Version version) throws InvalidMessageException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ZERO);
        String lengthString = Integer.toHexString(length);
        byte[] lengthBytes = lengthString.getBytes(StandardCharsets.UTF_8);
        int padding = version.lengthSize() - lengthBytes.length;
        while (padding-- > 0) {
            baos.write(ZERO);
        }

        try {
            baos.write(lengthBytes);
        } catch (IOException e) {
            throw new InvalidMessageException("Invalid message: unable to write length [" + length + "].");
        }

        return baos.toByteArray();
    }

    private byte[] writeToken(final Message message, final boolean ciphered) throws InvalidMessageException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(QUOTE);
            if (ciphered && message.type().mayBeCiphered()) {
                baos.write(STAR);
            }
            baos.write(message.getId().getBytes(StandardCharsets.UTF_8));
            baos.write(QUOTE);
        } catch (final Throwable e) {
            throw new InvalidMessageException(
                message.getAccountNumber(),
                "Invalid message: unable to write given message ID [" + message.getId() + "]."
            );
        }

        return baos.toByteArray();
    }

    private void writeSequence(final Message message, final Dc09Version version, final ByteArrayDataOutput out) throws InvalidMessageException {

        int sequence = message.getSequence();
        if (sequence == -1) {
            sequence = 0;
        }

        if (sequence >= 0 && sequence <= 9999) {
            String sequenceString = String.format("%04d", sequence);
            byte[] bytes = sequenceString.getBytes(StandardCharsets.UTF_8);
            if (bytes.length != version.seqSize()) {
                throw new InvalidMessageException(message.getAccountNumber(), "Invalid message: invalid sequence size [" + version.seqSize() + "].");
            }
            out.write(bytes);
        } else {
            throw new InvalidMessageException(message.getAccountNumber(), "Invalid message: unable to write sequence number [" + sequence + "].");
        }


    }

    private void writeReceiverNumber(final Message message, final Dc09GlobalParameters global, final ByteArrayDataOutput out) {


        Optional<String> optReceiverNumber = message.getReceiverNumber();
        if (optReceiverNumber.isPresent()) {
            out.write(R);
            String receiverNumber = optReceiverNumber.get();
            if (global.isHexToUpper()) {
                receiverNumber = receiverNumber.toUpperCase();
            }
            out.write(receiverNumber.getBytes(StandardCharsets.UTF_8));
        }

    }

    private void writeAccountPrefix(final Message message, final Dc09GlobalParameters global, final ByteArrayDataOutput out) {

        out.write(L);
        String accountPrefix = message.getAccountPrefix() != null ? message.getAccountPrefix() : "0";
        if (global.isHexToUpper()) {
            accountPrefix = accountPrefix.toUpperCase();
        }
        out.write(accountPrefix.getBytes(StandardCharsets.UTF_8));

    }

    private void writeAccountNumber(final Message message, final Dc09GlobalParameters global, final ByteArrayDataOutput out) {

        out.write(message.type() != MessageType.NAK ? DASH : A);
        String accountNumber = message.getAccountNumber();
        if (global.isHexToUpper()) {
            accountNumber = accountNumber.toUpperCase();
        }
        out.write(accountNumber.getBytes(StandardCharsets.UTF_8));

    }

    private void writeAccountDataNumber(final DataMessage message, final Dc09GlobalParameters global, final ByteArrayDataOutput out) {

        out.write(DASH);
        String accountDataNumber = message.getAccountNumberData();
        if (global.isHexToUpper()) {
            accountDataNumber = accountDataNumber.toUpperCase();
        }
        out.write(accountDataNumber.getBytes(StandardCharsets.UTF_8));

    }

    private final static class AdditionalDataHolder {

        private final List<AdditionalData> additionalData;
        private final String mac;

        AdditionalDataHolder(final List<AdditionalData> additionalData, final String mac) {
            this.additionalData = additionalData;
            this.mac = mac;
        }
    }

}
