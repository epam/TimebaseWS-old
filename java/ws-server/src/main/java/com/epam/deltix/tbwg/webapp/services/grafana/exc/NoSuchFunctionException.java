package com.epam.deltix.tbwg.webapp.services.grafana.exc;

public class NoSuchFunctionException extends ValidationException {

    private final String id;

    public NoSuchFunctionException(String id) {
        this.id = id;
    }

    @Override
    public String getMessage() {
        return "No function with id " + id + " was set.";
    }
}
