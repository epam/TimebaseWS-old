package com.epam.deltix.tbwg.webapp.services.charting.queries;

import io.reactivex.Observable;

public interface LineResult {

    String                  getName();

    long                    getAggregation();

    long                    getNewWindowSize();

    Observable<?>           getPoints();

}
