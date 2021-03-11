package com.epam.deltix.tbwg.webapp.services.grafana.exc;

public class SymbolNotSetException extends ValidationException {

    private final String functionName;

    public SymbolNotSetException(String functionName) {
        this.functionName = functionName;
    }

    @Override
    public String getMessage() {
        return String.format("Function %s requires set symbol. Please, set symbol in symbols select to proceed.", functionName);
    }
}
