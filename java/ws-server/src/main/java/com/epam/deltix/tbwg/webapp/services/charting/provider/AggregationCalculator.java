package com.epam.deltix.tbwg.webapp.services.charting.provider;

import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;

public interface AggregationCalculator {

    long            getAggregation(TimeInterval interval);

    long            getNewWindowSize(TimeInterval interval);

}
