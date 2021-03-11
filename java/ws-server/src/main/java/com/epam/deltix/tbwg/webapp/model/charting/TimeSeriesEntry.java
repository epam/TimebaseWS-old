package com.epam.deltix.tbwg.webapp.model.charting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import java.util.List;

/**
 * @author Daniil Yarmalkevich
 * Date: 8/19/2019
 */
public class TimeSeriesEntry {

    public TimeSeriesEntry() {}

    public TimeSeriesEntry(String target) {
        this.target = target;
        this.datapoints = new ObjectArrayList<>();
    }

    @JsonProperty
    public String target;

    @JsonProperty
    public List<Number[]> datapoints;

}
