package com.healthstream.orchestrator.controller;

import com.healthstream.orchestrator.model.InferenceRequest;
import com.healthstream.orchestrator.service.JobOrchestrator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobOrchestrator jobOrchestrator;

    public JobController(JobOrchestrator jobOrchestrator) {
        this.jobOrchestrator = jobOrchestrator;
    }

    /**
     * POST http://localhost:8080/api/v1/jobs/inference
     */
    @PostMapping("/inference")
    public String triggerInference(@RequestBody InferenceRequest request) {
        return jobOrchestrator.runInference(request);
    }
}