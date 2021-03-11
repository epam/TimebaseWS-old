package com.epam.deltix.tbwg.messages;

import com.epam.deltix.timebase.api.messages.service.FeedStatus;

public class FeedStatusMessage implements Message {

    private final long timestamp;
    private final long exchangeId;
    private final FeedStatus status;

    public FeedStatusMessage(long timestamp, long exchangeId, FeedStatus status) {
        this.timestamp = timestamp;
        this.exchangeId = exchangeId;
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getExchangeId() {
        return exchangeId;
    }

    public FeedStatus getStatus() {
        return status;
    }
}
