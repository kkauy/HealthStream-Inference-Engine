package com.healthstream.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthstream.orchestrator.model.InferenceRequest;
import com.healthstream.orchestrator.model.RelayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class RelayValidationService {

    private static final Logger log = LoggerFactory.getLogger(RelayValidationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RelayResponse validate(InferenceRequest request) {
        Process process = null;

        try {
            File relayExecutable = resolveRelayExecutable();
            log.info("Resolved relay executable path: {}", relayExecutable.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(relayExecutable.getAbsolutePath());
            pb.redirectErrorStream(false);

            process = pb.start();

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {

                String jsonRequest = objectMapper.writeValueAsString(request);
                log.info("Sending request to C++ relay: {}", jsonRequest);

                writer.write(jsonRequest);
                writer.newLine();
                writer.flush();
            }

            String stdout;
            String stderr;

            try (BufferedReader outReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                 BufferedReader errReader = new BufferedReader(
                         new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {

                stdout = outReader.readLine();
                stderr = errReader.readLine();
            }

            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("C++ relay timed out");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("C++ relay failed. exitCode=" + exitCode + ", stderr=" + stderr);
            }

            if (stdout == null || stdout.isBlank()) {
                throw new RuntimeException("C++ relay returned empty response");
            }

            log.info("C++ relay response: {}", stdout);
            return objectMapper.readValue(stdout, RelayResponse.class);

        } catch (Exception e) {
            log.error("Relay validation failed", e);

            RelayResponse error = new RelayResponse();
            error.setOk(false);
            error.setError("relay validation exception: " + e.getMessage());
            return error;

        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private File resolveRelayExecutable() {
        String currentDir = System.getProperty("user.dir");

        File[] candidates = new File[] {
                new File(currentDir, "native-relay-cpp/build/relay"),
                new File(currentDir, "../native-relay-cpp/build/relay")
        };

        for (File candidate : candidates) {
            log.info("Checking relay candidate path: {}", candidate.getAbsolutePath());
            if (candidate.exists() && candidate.isFile() && candidate.canExecute()) {
                return candidate;
            }
        }

        StringBuilder tried = new StringBuilder();
        for (File candidate : candidates) {
            tried.append(candidate.getAbsolutePath()).append(" ; ");
        }

        throw new RuntimeException("Relay executable not found in expected paths: " + tried);
    }
}