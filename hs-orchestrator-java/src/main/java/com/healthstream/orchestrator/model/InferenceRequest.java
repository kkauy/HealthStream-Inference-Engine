package com.healthstream.orchestrator.model;

import java.util.List;

public class InferenceRequest {
    private String id;
    private String task;
    private List<Double> features;

    public InferenceRequest() {}

    public InferenceRequest(String id, String task, List<Double> features) {
        this.id = id;
        this.task = task;
        this.features = features;
    }

    public String getId() {
        return id;
    }

    public String getTask() {
        return task;
    }

    public List<Double> getFeatures() {
        return features;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public void setFeatures(List<Double> features) {
        this.features = features;
    }
}