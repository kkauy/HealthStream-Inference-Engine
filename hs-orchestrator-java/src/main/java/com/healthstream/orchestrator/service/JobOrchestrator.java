package com.healthstream.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthstream.orchestrator.model.InferenceRequest;
import com.healthstream.orchestrator.model.RelayResponse;
import com.healthstream.orchestrator.util.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class JobOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(JobOrchestrator.class);

    private final RelayValidationService relayValidationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobOrchestrator(RelayValidationService relayValidationService) {
        this.relayValidationService = relayValidationService;
    }

    public String runInference(InferenceRequest request) {
        // Step 1: Validate with C++ relay
        RelayResponse relayResponse = relayValidationService.validate(request);

        if (!relayResponse.isOk()) {
            return "{\"ok\":false,\"stage\":\"relay\",\"error\":\"" + escapeJson(relayResponse.getError()) + "\"}";
        }

        // Step 2: If relay passed, then call Python worker
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        Process process = null;
        Thread gobblerThread = null;

        try {
            String currentDir = System.getProperty("user.dir");
            File workerDir = new File(currentDir, "inference-workers");

            ProcessBuilder pb = new ProcessBuilder(
                    "python3", "inference_worker.py"
            );

            pb.directory(workerDir);
            pb.redirectErrorStream(true);

            log.info("Starting Python inference for requestId={}", request.getId());
            process = pb.start();

            // send JSON to python via stdin
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {

                String jsonRequest = objectMapper.writeValueAsString(request);
                writer.write(jsonRequest);
                writer.newLine();
                writer.flush();
            }

            StreamGobbler gobbler = new StreamGobbler(process.getInputStream(), results::add);
            gobblerThread = new Thread(gobbler);
            gobblerThread.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                log.warn("Inference timeout for requestId={}. Killing python process...", request.getId());

                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }

                return "{\"ok\":false,\"stage\":\"python\",\"error\":\"timeout\",\"message\":\"Python worker exceeded 30s\"}";
            }

            int exitCode = process.exitValue();

            if (gobblerThread != null) {
                gobblerThread.join(2000);
            }

            if (exitCode == 0) {
                return String.join("\n", results);
            } else {
                return "{\"ok\":false,\"stage\":\"python\",\"error\":\"Process failed with exit code " + exitCode + "\"}";
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "{\"ok\":false,\"error\":\"interrupted\",\"message\":\"Orchestrator interrupted\"}";

        } catch (Exception e) {
            log.error("Inference failed for requestId={}", request.getId(), e);
            return "{\"ok\":false,\"error\":\"exception\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}";

        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}