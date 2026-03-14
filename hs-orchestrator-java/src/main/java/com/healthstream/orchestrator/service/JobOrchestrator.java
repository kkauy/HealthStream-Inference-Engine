package com.healthstream.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthstream.orchestrator.model.InferenceRequest;
import com.healthstream.orchestrator.model.RelayResponse;
import com.healthstream.orchestrator.util.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class JobOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(JobOrchestrator.class);

    private final RelayValidationService relayValidationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobOrchestrator(RelayValidationService relayValidationService,
                           RedisTemplate<String, String> redisTemplate) {
        this.relayValidationService = relayValidationService;
        this.redisTemplate = redisTemplate;
    }

    public String runInference(InferenceRequest request) {


        // STEP 0: Redis Cache Check
        // Checking before relay validation and Python spawn — cheapest path first

        String cacheKey = "infer:" + request.getTask()
                + ":" + sha256(request.getFeatures().toString());

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("[CACHE HIT] requestId={} key={}", request.getId(), cacheKey);
            return cached;
        }
        log.info("[CACHE MISS] requestId={} — proceeding to relay + worker", request.getId());

        // Step 1: Validate with C++ relay

        RelayResponse relayResponse = relayValidationService.validate(request);

        if (!relayResponse.isOk()) {
            return "{\"ok\":false,\"stage\":\"relay\",\"error\":\"" + escapeJson(relayResponse.getError()) + "\"}";
        }

        // Step 2: Spawn Python worker
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        Process process = null;
        Thread gobblerThread = null;

        try {
            String currentDir = System.getProperty("user.dir");

            File workerDir = new File(currentDir, "inference-workers");
            if (!workerDir.exists() || !workerDir.isDirectory()) {
                workerDir = new File(currentDir, "../inference-workers");
            }

            if (!workerDir.exists() || !workerDir.isDirectory()) {
                throw new RuntimeException("Python worker directory not found. Tried: "
                        + new File(currentDir, "inference-workers").getAbsolutePath()
                        + " and "
                        + new File(currentDir, "../inference-workers").getAbsolutePath());
            }

            File workerScript = new File(workerDir, "inference_worker.py");
            if (!workerScript.exists() || !workerScript.isFile()) {
                throw new RuntimeException("Python worker script not found: " + workerScript.getAbsolutePath());
            }

            log.info("Resolved worker directory: {}", workerDir.getAbsolutePath());
            log.info("Resolved worker script: {}", workerScript.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder("python3", workerScript.getAbsolutePath());
            pb.directory(workerDir);
            pb.redirectErrorStream(true);

            log.info("Starting Python inference for requestId={}", request.getId());
            process = pb.start();

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                String jsonRequest = objectMapper.writeValueAsString(request);
                log.info("Sending request to Python worker: {}", jsonRequest);
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

            String processOutput = String.join("\n", results);

            if (exitCode == 0) {
                // STORE RESULT in Redis (NEW) — only cache successful results
                redisTemplate.opsForValue().set(cacheKey, processOutput, 30, TimeUnit.MINUTES);
                log.info("[CACHE STORED] requestId={} key={} ttl=30min", request.getId(), cacheKey);
                return processOutput;
            } else {
                return "{\"ok\":false,\"stage\":\"python\",\"error\":\"Process failed with exit code "
                        + exitCode + "\",\"details\":\"" + escapeJson(processOutput) + "\"}";
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

    // SHA-256 helper — short 16-char hex key for Redis
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString().substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}