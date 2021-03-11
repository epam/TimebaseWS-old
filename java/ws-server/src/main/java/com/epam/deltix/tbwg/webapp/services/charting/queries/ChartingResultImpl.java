package com.epam.deltix.tbwg.webapp.services.charting.queries;

import java.util.ArrayList;
import java.util.List;

public class ChartingResultImpl implements ChartingResult {

    private final List<LinesQueryResult> results = new ArrayList<>();
    private final Runnable runnable;

    public ChartingResultImpl(List<LinesQueryResult> results, Runnable runnable) {
        this.results.addAll(results);
        this.runnable = runnable;
    }

    @Override
    public List<LinesQueryResult> results() {
        return results;
    }

    @Override
    public void run() {
        runnable.run();
    }
}
