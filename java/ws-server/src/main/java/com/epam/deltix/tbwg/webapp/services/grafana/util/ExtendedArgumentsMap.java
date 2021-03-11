package com.epam.deltix.tbwg.webapp.services.grafana.util;

import com.epam.deltix.computations.data.ArgumentsMap;

public class ExtendedArgumentsMap extends ArgumentsMap implements ExtendedArguments {

    public ExtendedArgumentsMap() {
        super();
    }

    public ExtendedArgumentsMap(long start, long end, long interval, String symbol, String result) {
        super(start, end, interval, symbol, result);
    }
}
