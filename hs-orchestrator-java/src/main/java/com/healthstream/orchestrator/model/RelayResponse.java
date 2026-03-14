package com.healthstream.orchestrator.model;

public class RelayResponse {
    private boolean ok;
    private String id;
    private String message;
    private String error;

    public RelayResponse() {}

    public boolean isOk() {
        return ok;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setError(String error) {
        this.error = error;
    }
}