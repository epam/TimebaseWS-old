package com.epam.deltix.tbwg.webapp.services.charting.queries;

import com.epam.deltix.tbwg.webapp.model.charting.ChartType;
import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;

public interface LinesQuery {

    ChartType           getType();

    TimeInterval        getInterval();

    int                 getMaxPointsCount();

}
