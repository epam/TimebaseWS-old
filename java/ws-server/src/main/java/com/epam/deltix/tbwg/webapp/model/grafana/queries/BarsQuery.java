package com.epam.deltix.tbwg.webapp.model.grafana.queries;

public class BarsQuery extends TBQuery {

    protected String symbol;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
