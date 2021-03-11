package com.epam.deltix.tbwg.webapp.services.grafana.exc;

public class NoSuchStreamException extends ValidationException {

    private final String key;

    public NoSuchStreamException(String key) {
        this.key = key;
    }

    @Override
    public String getMessage() {
        return String.format("No stream %s.", key);
    }

}
