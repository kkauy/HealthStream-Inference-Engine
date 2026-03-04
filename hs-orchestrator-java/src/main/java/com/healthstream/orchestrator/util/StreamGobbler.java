package com.healthstream.orchestrator.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

// Prevent Java deadlock
public class StreamGobbler implements Runnable {
    private final InputStream inputStream;
    private final Consumer<String> consumer;
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(StreamGobbler.class);
    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
        this.inputStream = inputStream;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    consumer.accept(line);
                } catch (RuntimeException re) {
                    log.warn("StreamGobbler consumer failed to handle line", re);
                    continue;
                }
            }
        } catch (java.io.IOException ioe) {
            log.warn("StreamGobbler failed to read process stream", ioe);
        }
    }
}