package com.epam.deltix.tbwg.webapp.services.charting.queries;

import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;

import java.util.List;

public interface LinesQueryResult {

    String                  getName();

    List<LineResult>        getLines();

    TimeInterval            getInterval();

}
