package com.healthstream.orchestrator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service to orchestrate ML inference tasks by invoking Python workers.
 * Implements process isolation and resource concurrency control.
 */
@Service
public class JobOrchestrator {

    // Manual logger declaration to bypass Lombok annotation issues
    private static final Logger log = LoggerFactory.getLogger(JobOrchestrator.class);

    // Bounding the thread pool to 4 to prevent CPU/Memory exhaustion from ML tasks
    private final ExecutorService executor = Executors.newFixedThreadPool(4);


    /**
     * Executes the Python ML worker and captures its output.
     * Uses dynamic path discovery to find the 'inference-workers' directory
     * relative to the Java application's location.
     */
    public String runInference(String patientId) {
        StringBuilder output = new StringBuilder();
        try {
            // 1. Get the ROOT directory (HealthStream-Inference-Engine)
            String rootPath = System.getProperty("user.dir");
            File rootDir = new File(rootPath);

            // 2. Locate the worker folder directly inside the root
            File workerDir = new File(rootDir, "inference-workers");

            // 3. Command setup
            ProcessBuilder pb = new ProcessBuilder(
                    "python3",
                    "inference_worker.py",
                    "--task", "breast_cancer",
                    "--input", patientId
            );

            pb.directory(workerDir);
            pb.redirectErrorStream(true);

            log.info("Executing Python from: " + workerDir.getAbsolutePath());

            Process process = pb.start();

            // 4. Read output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            return (exitCode == 0) ? output.toString() : "{\"error\": \"Exit code " + exitCode + "\"}";

        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

}