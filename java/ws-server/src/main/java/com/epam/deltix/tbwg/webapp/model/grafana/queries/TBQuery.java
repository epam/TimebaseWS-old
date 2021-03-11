package com.epam.deltix.tbwg.webapp.model.grafana.queries;

public class TBQuery extends DataQuery {

    protected String stream;

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }
}
