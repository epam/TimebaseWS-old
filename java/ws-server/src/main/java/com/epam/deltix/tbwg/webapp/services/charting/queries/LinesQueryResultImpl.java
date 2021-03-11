package com.epam.deltix.tbwg.webapp.services.charting.queries;

import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;

import java.util.ArrayList;
import java.util.List;

public class LinesQueryResultImpl implements LinesQueryResult {

    private final String name;
    private final List<LineResult> lines = new ArrayList<>();
    private final TimeInterval interval;

    public LinesQueryResultImpl(String name, TimeInterval interval) {
        this.name = name;
        this.interval = interval;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<LineResult> getLines() {
        return lines;
    }

    @Override
    public TimeInterval getInterval() {
        return interval;
    }

}
