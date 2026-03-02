package com.healthstream.orchestrator.controller;

import com.healthstream.orchestrator.service.JobOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    @Autowired
    private JobOrchestrator jobOrchestrator;

    /**
     * Endpoint to trigger ML inference for a specific patient.
     * Use: POST http://localhost:8080/api/v1/jobs/inference/{patientId}
     */
    @PostMapping("/inference/{patientId}")
    public String triggerInference(@PathVariable String patientId) {
        String pythonResult = jobOrchestrator.runInference(patientId);
        return pythonResult;
    }
}