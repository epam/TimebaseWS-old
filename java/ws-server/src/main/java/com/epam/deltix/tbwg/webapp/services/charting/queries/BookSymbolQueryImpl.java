package com.epam.deltix.tbwg.webapp.services.charting.queries;

import com.epam.deltix.tbwg.webapp.model.charting.ChartType;
import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;

public class BookSymbolQueryImpl extends SymbolQueryImpl implements BookSymbolQuery {

    private final int levelsCount;

    public BookSymbolQueryImpl(String stream, String symbol, ChartType type, TimeInterval interval, int maxPointsCount, int levels) {
        super(stream, symbol, type, interval, maxPointsCount);

        this.levelsCount = levels;
    }

    @Override
    public int getLevelsCount() {
        return levelsCount;
    }
}
