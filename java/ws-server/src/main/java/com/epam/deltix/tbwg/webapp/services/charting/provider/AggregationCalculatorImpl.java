package com.epam.deltix.tbwg.webapp.services.charting.provider;

import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;

public class AggregationCalculatorImpl implements AggregationCalculator {

    private final long maxPointsPerLine;
    private final double zoomDetailsFactor;

    public AggregationCalculatorImpl(long maxPointsPerLine, double zoomDetailsFactor) {
        this.maxPointsPerLine = maxPointsPerLine;
        this.zoomDetailsFactor = zoomDetailsFactor;
    }

    @Override
    public long getAggregation(TimeInterval interval) {
        return (interval.getEndTimeMilli() - interval.getStartTimeMilli()) / maxPointsPerLine;
    }

    @Override
    public long getNewWindowSize(TimeInterval interval) {
        return (long) (maxPointsPerLine * (zoomDetailsFactor * ((float) (interval.getEndTimeMilli() - interval.getStartTimeMilli()) / maxPointsPerLine)));
    }


}
