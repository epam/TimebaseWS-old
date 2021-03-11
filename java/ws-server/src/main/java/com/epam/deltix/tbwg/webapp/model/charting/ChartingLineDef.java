package com.epam.deltix.tbwg.webapp.model.charting;

import com.epam.deltix.tbwg.webapp.model.charting.line.LineElement;

import java.util.List;

/**
 * The charting data for a line.
 */
public class ChartingLineDef {
    private final long aggregationSizeMs;
    private final long newWindowSizeMs;
    private final List<LineElement> line;

    public ChartingLineDef(long aggregationSizeMs, long newWindowSizeMs, List<LineElement> line) {
        this.aggregationSizeMs = aggregationSizeMs;
        this.newWindowSizeMs = newWindowSizeMs;
        this.line = line;
    }

    /**
     * The size of aggregation (in milliseconds) used to create the line.
     */
    public long getAggregationSizeMs() {
        return aggregationSizeMs;
    }

    /**
     * The size of a charting window (in milliseconds) for which the new line detalization is available.
     */
    public long getNewWindowSizeMs() {
        return newWindowSizeMs;
    }

    /**
     * The list of line points.
     */
    public List<LineElement> getPoints() {
        return line;
    }
}
