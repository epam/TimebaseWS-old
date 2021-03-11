package com.epam.deltix.tbwg.webapp.services.charting.queries;

import java.util.List;

public interface ChartingResult extends Runnable {

    List<LinesQueryResult>      results();

}
