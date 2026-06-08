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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Logs incoming raw DC-09 frames to a text file for later playback.
 *
 * <p>Configuration is read from {@code server-log.properties} on the classpath:</p>
 * <pre>
 *   message.log.enabled = true/false
 *   message.log.file    = incoming-messages.log
 *   message.log.max.mb  = 10   (0 = no rotation)
 * </pre>
 *
 * <p>Each line written has the format:</p>
 * <pre>
 *   &lt;ISO-8601 timestamp&gt;|&lt;host:port&gt;|&lt;raw ASCII DC-09 frame&gt;
 * </pre>
 */
class IncomingMessageLogger implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IncomingMessageLogger.class);

    private static final String CONFIG_RESOURCE = "server-log.properties";
    private static final String PROP_ENABLED    = "message.log.enabled";
    private static final String PROP_FILE       = "message.log.file";
    private static final String PROP_MAX_MB     = "message.log.max.mb";

    private final AtomicBoolean enabled;
    private final Path logPath;
    private final long maxBytes;

    private BufferedWriter writer;

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Loads configuration from {@code server-log.properties} on the classpath
     * and returns a ready-to-use logger.
     */
    static IncomingMessageLogger fromConfig() {
        Properties props = new Properties();
        try (InputStream is = IncomingMessageLogger.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_RESOURCE)) {
            if (is != null) {
                props.load(is);
            } else {
                LOG.warn("'{}' not found on classpath – message file logging disabled.", CONFIG_RESOURCE);
            }
        } catch (IOException e) {
            LOG.warn("Failed to read '{}': {} – message file logging disabled.", CONFIG_RESOURCE, e.getMessage());
        }

        boolean enabled = Boolean.parseBoolean(props.getProperty(PROP_ENABLED, "false"));
        String  file    = props.getProperty(PROP_FILE, "incoming-messages.log");
        long    maxMb   = Long.parseLong(props.getProperty(PROP_MAX_MB, "10"));

        return new IncomingMessageLogger(enabled, Paths.get(file), maxMb);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    IncomingMessageLogger(final boolean enabled, final Path logPath, final long maxMb) {
        this.enabled  = new AtomicBoolean(enabled);
        this.logPath  = logPath;
        this.maxBytes = maxMb * 1024 * 1024;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Opens the log file (appending). Call once before {@link #log}. */
    synchronized void open() throws IOException {
        if (writer != null) return;
        writer = Files.newBufferedWriter(logPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        writeLine("# SIA DC-09 incoming message log — started " + Instant.now());
        LOG.info("Message file logger opened: {} (enabled={})", logPath.toAbsolutePath(), enabled.get());
    }

    /** Closes the log file. */
    @Override
    public synchronized void close() {
        if (writer == null) return;
        try {
            writeLine("# Log closed — " + Instant.now());
            writer.close();
        } catch (IOException e) {
            LOG.warn("Error closing message log: {}", e.getMessage());
        } finally {
            writer = null;
        }
    }

    // ── Runtime toggle ────────────────────────────────────────────────────────

    /** Enables or disables logging at runtime without closing the file. */
    void setEnabled(final boolean value) {
        enabled.set(value);
        LOG.info("Message file logging {}.", value ? "ENABLED" : "DISABLED");
    }

    boolean isEnabled() {
        return enabled.get();
    }

    // ── Logging ───────────────────────────────────────────────────────────────

    /**
     * Appends one incoming raw DC-09 frame to the log file.
     *
     * @param address the remote {@code host:port} string
     * @param rawAscii the raw DC-09 frame as an ASCII string
     */
    synchronized void log(final String address, final String rawAscii) {
        if (!enabled.get() || writer == null) return;
        try {
            rotateIfNeeded();
            writeLine(Instant.now() + "|" + address + "|" + rawAscii);
        } catch (IOException e) {
            LOG.warn("Failed to write message log entry: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void writeLine(final String line) throws IOException {
        writer.write(line);
        writer.newLine();
        writer.flush();
    }

    private void rotateIfNeeded() throws IOException {
        if (maxBytes <= 0) return;
        if (!Files.exists(logPath)) return;
        if (Files.size(logPath) < maxBytes) return;

        writer.close();
        Path rotated = logPath.resolveSibling(
                logPath.getFileName() + "." + Instant.now().toEpochMilli());
        Files.move(logPath, rotated);
        LOG.info("Message log rotated to: {}", rotated);
        writer = Files.newBufferedWriter(logPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        writeLine("# SIA DC-09 incoming message log — rotated " + Instant.now());
    }
}
