package com.epam.deltix.tbwg.webapp.services.grafana.exc;

import java.util.List;

public class NoSuchSymbolsException extends ValidationException {

    private final String stream;
    private final List<String> symbols;

    public NoSuchSymbolsException(String stream, List<String> symbols) {
        this.stream = stream;
        this.symbols = symbols;
    }

    @Override
    public String getMessage() {
        return String.format("No symbols %s in stream %s.", symbols, stream);
    }

}