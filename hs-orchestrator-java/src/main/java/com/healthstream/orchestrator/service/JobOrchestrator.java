package com.healthstream.orchestrator.service;

import com.healthstream.orchestrator.util.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class JobOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(JobOrchestrator.class);

    public String runInference(String patientId) {
        // Use a thread-safe list to collect output, as it will be populated by a separate asynchronous thread
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        Process process = null;
        Thread gobblerThread = null;

        try {
            // 1. Resolve the directory where the Python scripts are located
            String currentDir = System.getProperty("user.dir");
            File workerDir = new File(currentDir, "inference-workers");

            // 2. Configure the ProcessBuilder to run the Python inference script
            ProcessBuilder pb = new ProcessBuilder(
                    "python3", "inference_worker.py",
                    "--task", "breast_cancer",
                    "--input", patientId
            );

            // Set the working directory for the Python process
            pb.directory(workerDir);

            // Merge the error stream into the standard output stream to capture all logs in one place
            pb.redirectErrorStream(true);

            log.info("Starting Python Inference for patient: {}", patientId);
            process = pb.start();

            // 3. Start the StreamGobbler on a separate thread.
            // This is CRITICAL to prevent OS pipe deadlocks by continuously consuming the Python process's output.
            StreamGobbler gobbler = new StreamGobbler(process.getInputStream(), results::add);
            gobblerThread = new Thread(gobbler);
            gobblerThread.start();

            // 4. Wait for the Python process to finish execution
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                log.warn("Inference timeout for patientId={}. Killing python process ... ", patientId);

                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }

                return "{\"error\":\"timeout\",\"message\":\"Python worker exceeded 30s\"}";
            }

            int exitCode = process.exitValue();

            // Wait up to 2000ms (2 seconds) for the gobbler thread to finish processing the last bits of output
            if (gobblerThread != null) {
                gobblerThread.join(2000);
            }

            // 5. Return the result or format a JSON error message based on the exit code
            if (exitCode == 0) {
                return String.join("\n", results);
            } else {
                return "{\"error\": \"Process failed with exit code " + exitCode + "\"}";
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "{\"error\":\"interrupted\",\"message\":\"Orchestrator interrupted\"}";

        } catch (Exception e) {
            log.error("Inference failed for patientId={}", patientId, e);
            return "{\"error\":\"exception\",\"message\":\"" + e.getMessage() + "\"}";

        } finally {
            if (process != null && process. isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}