package com.epam.deltix.tbwg.webapp.model.grafana.time;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.webcohesion.enunciate.metadata.DocumentationExample;

import java.time.Instant;

import static com.epam.deltix.tbwg.webapp.utils.DateFormatter.DATETIME_MILLIS_FORMAT_STR;

public class TimeRange {

    @DocumentationExample("2016-10-31T06:33:44.866Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATETIME_MILLIS_FORMAT_STR, timezone = "UTC")
    @JsonProperty
    protected Instant from;

    @DocumentationExample("2016-10-31T12:33:44.866Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATETIME_MILLIS_FORMAT_STR, timezone = "UTC")
    @JsonProperty
    protected Instant to;

    @JsonProperty
    protected RawTimeRange raw;

    public Instant getFrom() {
        return from;
    }

    public void setFrom(Instant from) {
        this.from = from;
    }

    public Instant getTo() {
        return to;
    }

    public void setTo(Instant to) {
        this.to = to;
    }

    public RawTimeRange getRaw() {
        return raw;
    }

    public void setRaw(RawTimeRange raw) {
        this.raw = raw;
    }
}
