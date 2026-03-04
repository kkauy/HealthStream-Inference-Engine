package com.healthstream.orchestrator.service;

import com.healthstream.orchestrator.util.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

@Service
public class JobOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(JobOrchestrator.class);

    public String runInference(String patientId) {
        // Use a thread-safe list to collect output, as it will be populated by a separate asynchronous thread
        List<String> results = Collections.synchronizedList(new ArrayList<>());

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
            Process process = pb.start();

            // 3. Start the StreamGobbler on a separate thread.
            // This is CRITICAL to prevent OS pipe deadlocks by continuously consuming the Python process's output.
            StreamGobbler gobbler = new StreamGobbler(process.getInputStream(), results::add);
            Thread gobblerThread = new Thread(gobbler);
            gobblerThread.start();

            // 4. Wait for the Python process to finish execution
            int exitCode = process.waitFor();

            // Wait up to 2000ms (2 seconds) for the gobbler thread to finish processing the last bits of output
            gobblerThread.join(2000);

            // 5. Return the result or format a JSON error message based on the exit code
            if (exitCode == 0) {
                return String.join("\n", results);
            } else {
                return "{\"error\": \"Process failed with exit code " + exitCode + "\"}";
            }

        } catch (Exception e) {
            log.error("Inference failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}