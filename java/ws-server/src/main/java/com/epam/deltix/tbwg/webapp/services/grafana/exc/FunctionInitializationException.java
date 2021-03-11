package com.epam.deltix.tbwg.webapp.services.grafana.exc;

public class FunctionInitializationException extends ValidationException {

    private final String id;

    public FunctionInitializationException(String id, Throwable cause) {
        super(cause);
        this.id = id;
    }

    @Override
    public String getMessage() {
        return "Failed to initialize function with id " + id + ". Cause: " + getCause();
    }
}
