package com.epam.deltix.tbwg.webapp.services.grafana.exc;

public class NoTargetsException extends ValidationException {

    @Override
    public String getMessage() {
        return "No selected targets.";
    }

}
