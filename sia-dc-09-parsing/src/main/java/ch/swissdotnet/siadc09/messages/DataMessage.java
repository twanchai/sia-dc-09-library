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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@code DataMessage} class extends {@code Message} by adding data and additional data.
 * <p/>
 * {@code DataMessage}s may be encrypted.
 *
 * @author Vincent Pasquier <vincent.pasquier at swissdotnet.ch>
 */
public abstract class DataMessage extends Message {

    // The data associated with message.
    private final String data;
    // The additional data associated with message.
    private final List<AdditionalData> additional = Lists.newArrayList();
    // The MAC address stored in additional message (might be null).
    private final String mac;
    // The message ID (i.e. SIA-DSC, NULL, ...).
    private final String id;

    DataMessage(final Builder builder) {
        super(builder);
        this.data = builder.data;
        this.additional.addAll(builder.additional);
        this.mac = builder.mac;
        this.id = builder.id;
    }

    /**
     * @return the data associated with message
     */
    public String getData() {
        return data;
    }

    /**
     * @return the additional data associated with message
     */
    public List<AdditionalData> getAdditional() {
        return additional;
    }

    /**
     * @return the optionally present MAC address
     */
    public Optional<String> getMacAddress() {
        return Optional.ofNullable(mac);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .omitNullValues()
            .add("message", super.toString())
            .add("data", data)
            .add("additional", additional)
            .add("id", id)
            .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof final DataMessage that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(data, that.data)
            && Objects.equals(additional, that.additional)
            && Objects.equals(mac, that.mac)
            && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), data, additional, mac, id);
    }
}
