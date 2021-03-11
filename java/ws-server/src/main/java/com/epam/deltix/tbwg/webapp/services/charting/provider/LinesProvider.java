package com.epam.deltix.tbwg.webapp.services.charting.provider;

import com.epam.deltix.tbwg.webapp.services.charting.queries.ChartingResult;
import com.epam.deltix.tbwg.webapp.services.charting.queries.LinesQuery;
import com.epam.deltix.tbwg.webapp.services.charting.queries.LinesQueryResult;

import java.util.List;

public interface LinesProvider {

    ChartingResult          getLines(List<LinesQuery> query);

}
