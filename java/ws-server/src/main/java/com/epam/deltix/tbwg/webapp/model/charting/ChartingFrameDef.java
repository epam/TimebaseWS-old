package com.epam.deltix.tbwg.webapp.model.charting;

import com.epam.deltix.tbwg.webapp.model.TimeRangeDef;
import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;

import java.util.Map;

/**
 * The container for charting getQuery result.
 * @label ChartingFrame
 */
public class ChartingFrameDef {
    private final String name;
    private final Map<String, ChartingLineDef> lines;
    private final TimeRangeDef effectiveWindow;

    public ChartingFrameDef(String name, Map<String, ChartingLineDef> lines, final TimeInterval effectiveWindow) {
        this.name = name;
        this.lines = lines;
        this.effectiveWindow = new TimeRangeDef(effectiveWindow.getStartTime(), effectiveWindow.getEndTime());
    }

    public String getName() {
        return name;
    }

    /**
     * The collection of charting lines.
     */
    public Map<String, ChartingLineDef> getLines() {
        return lines;
    }

    /**
     * The effective window where charting lines were calculated.
     * Effective window might be different from the requested window due to interval enlargement.
     */
    public TimeRangeDef getEffectiveWindow() {
        return effectiveWindow;
    }
}
