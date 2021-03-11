package com.epam.deltix.tbwg.webapp.model.grafana.queries;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.epam.deltix.tbwg.webapp.model.grafana.time.TimeRange;

import java.util.List;

public class DataQueryRequest<T extends DataQuery> {

    @JsonProperty
    protected String requestId;

    @JsonProperty
    protected long dashboardId;

    @JsonProperty
    protected String interval;

    @JsonProperty
    protected Long intervalMs;

    @JsonProperty
    protected Integer maxDataPoints;

    @JsonProperty
    protected String panelId;

    @JsonProperty
    protected TimeRange range;

    @JsonProperty
    protected Boolean reverse;

    @JsonProperty
    protected List<T> targets;

    @JsonProperty
    protected String timezone;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public long getDashboardId() {
        return dashboardId;
    }

    public void setDashboardId(long dashboardId) {
        this.dashboardId = dashboardId;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public Long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(Long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public Integer getMaxDataPoints() {
        return maxDataPoints;
    }

    public void setMaxDataPoints(Integer maxDataPoints) {
        this.maxDataPoints = maxDataPoints;
    }

    public String getPanelId() {
        return panelId;
    }

    public void setPanelId(String panelId) {
        this.panelId = panelId;
    }

    public TimeRange getRange() {
        return range;
    }

    public void setRange(TimeRange range) {
        this.range = range;
    }

    public Boolean getReverse() {
        return reverse;
    }

    public void setReverse(Boolean reverse) {
        this.reverse = reverse;
    }

    public List<T> getTargets() {
        return targets;
    }

    public void setTargets(List<T> targets) {
        this.targets = targets;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
