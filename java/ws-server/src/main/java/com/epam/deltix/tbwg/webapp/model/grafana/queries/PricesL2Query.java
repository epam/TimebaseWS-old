package com.epam.deltix.tbwg.webapp.model.grafana.queries;

public class PricesL2Query extends TBQuery {

    protected String symbol;
    protected short levels;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public short getLevels() {
        return levels;
    }

    public void setLevels(short levels) {
        this.levels = levels;
    }
}
