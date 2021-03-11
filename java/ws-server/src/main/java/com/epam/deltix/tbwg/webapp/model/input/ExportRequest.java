package com.epam.deltix.tbwg.webapp.model.input;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.webcohesion.enunciate.metadata.DocumentationExample;

import java.time.Instant;

import static com.epam.deltix.tbwg.webapp.utils.DateFormatter.DATETIME_MILLIS_FORMAT_STR;

public class ExportRequest {

    /**
     * The start timestamp in UTC (inclusive), for example 2018-06-28T09:30:00.123Z
     */
    @DocumentationExample("2018-06-28T09:30:00.123Z")
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATETIME_MILLIS_FORMAT_STR, timezone = "UTC")
    public Instant from;

    /**
     * The end timestamp in UTC (inclusive), for example 2018-06-28T00:00:00.123Z
     */
    @DocumentationExample("2018-06-30T09:30:00.123Z")
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATETIME_MILLIS_FORMAT_STR, timezone = "UTC")
    public Instant              to;

    /**
     * Start row offset. (By default = 0)
     */
    @DocumentationExample("0")
    @JsonProperty
    public long                 offset = 0;

    /**
     * Number of returning rows. (By default = -1, means all rows must be selected)
     */
    @DocumentationExample("1000")
    @JsonProperty
    public int                  rows = -1;

    /**
     * Result order of messages
     */
    @DocumentationExample("false")
    @JsonProperty
    public boolean              reverse = false;

    @JsonIgnore
    public long                 getStartTime(long currentTime) {
        return from != null ? from.toEpochMilli() : Long.MIN_VALUE;
    }

    @JsonIgnore
    public long                 getEndTime() {
        return to != null ? to.toEpochMilli() : Long.MAX_VALUE;
    }

    /**
     * Specified message types to be subscribed. If undefined, then all types will be subscribed.
     */
    @DocumentationExample(value = "deltix.timebase.api.messages.TradeMessage", value2 = "deltix.timebase.api.messages.BestBidOfferMessage")
    @JsonProperty
    public String[]             types;

    /**
     * Specified instruments(symbols) to be subscribed. If undefined, then all instruments will be subscribed.
     */
    @DocumentationExample(value = "BTCEUR", value2 = "ETHEUR")
    @JsonProperty
    public String[]             symbols;

}