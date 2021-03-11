package com.epam.deltix.tbwg.webapp.model.grafana;

import java.util.List;

public class TBField {

    protected String name;

    protected List<AggregationInfo> aggregations;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AggregationInfo> getAggregations() {
        return aggregations;
    }

    public void setAggregations(List<AggregationInfo> aggregations) {
        this.aggregations = aggregations;
    }
}
