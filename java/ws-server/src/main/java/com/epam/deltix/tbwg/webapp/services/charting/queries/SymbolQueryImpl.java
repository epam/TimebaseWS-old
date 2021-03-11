package com.epam.deltix.tbwg.webapp.services.charting.queries;

import com.epam.deltix.tbwg.webapp.model.charting.ChartType;
import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;

public class SymbolQueryImpl implements SymbolQuery {

    private final String stream;
    private final String symbol;
    private final TimeInterval interval;
    private final int maxPointsCount;
    private final ChartType type;

    public SymbolQueryImpl(String stream, String symbol, ChartType type, TimeInterval interval, int maxPointsCount) {
        this.stream = stream;
        this.symbol = symbol;
        this.type = type;
        this.interval = interval;
        this.maxPointsCount = maxPointsCount;
    }

    @Override
    public String getStream() {
        return stream;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public ChartType getType() {
        return type;
    }

    @Override
    public TimeInterval getInterval() {
        return interval;
    }

    @Override
    public int getMaxPointsCount() {
        return maxPointsCount;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SymbolQuery ");
        sb.append(stream).append("[");
        sb.append(symbol).append('|');
        sb.append(interval).append('|');
        sb.append(type);
        sb.append(']');
        return sb.toString();
    }
}
