package main.java.com.healthstream;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        // Kafka Event
        String taskType = "breast_cancer";
        String inputData = "../data/test_sample.csv";

        System.out.println("[Java-Orchestrator] Starting Job Pipeline...");
        launchInferenceJob(taskType, inputData);
    }

    public static void launchInferenceJob(String task, String path) {
        try {
            // Path to the python script
            String pythonScriptPath = "../hs-worker-python/worker_wrapper.py";

            // Create ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder("python3", pythonScriptPath, "--task", task, "--input", path);
            pb.redirectErrorStream(true); // Merge error and output

            Process process = pb.start();

            // Read output from Python
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            System.out.println("[Java-Orchestrator] Logs from Python Worker:");
            while ((line = reader.readLine()) != null) {
                System.out.println("  > " + line);
            }

            int exitCode = process.waitFor();
            System.out.println("[Java-Orchestrator] Job finished with exit code: " + exitCode);

        } catch (Exception e) {
            System.err.println("Critical Error: Failed to launch inference job.");
            e.printStackTrace();
        }
    }
}