package com.epam.deltix.tbwg.webapp.model.grafana;

import com.epam.deltix.tbwg.webapp.model.charting.ChartType;

public enum GrafanaChartType {
    PRICES_L2,
    BARS,
    CUSTOM;

    public ChartType convertToChartType() {
        switch (this) {
            case BARS:
                return ChartType.BARS;
            case PRICES_L2:
                return ChartType.PRICES_L2;
            default:
                return null;
        }
    }
}
