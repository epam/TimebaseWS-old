package com.epam.deltix.tbwg.webapp.services.charting.queries;

import io.reactivex.Observable;

public class LineResultImpl implements LineResult {

    private final String name;
    private final Observable<?> points;
    private final long aggregation;
    private final long newWindowSize;

    public LineResultImpl(String name, Observable<?> points, long aggregation, long newWindowSize) {
        this.name = name;
        this.points = points;
        this.aggregation = aggregation;
        this.newWindowSize = newWindowSize;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getAggregation() {
        return aggregation;
    }

    @Override
    public long getNewWindowSize() {
        return newWindowSize;
    }

    @Override
    public Observable<?> getPoints() {
        return points;
    }
}
