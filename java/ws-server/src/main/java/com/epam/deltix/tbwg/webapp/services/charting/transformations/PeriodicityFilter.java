package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.timebase.messages.InstrumentMessage;

public class PeriodicityFilter {
    private volatile long periodicity;
    private final boolean allowFirst;

    private long lastMessageTime = Long.MIN_VALUE;

    public PeriodicityFilter(long periodicity) {
        this(periodicity, false);
    }

    public PeriodicityFilter(long periodicity, boolean allowFirst) {
        this.periodicity = periodicity;
        this.allowFirst = allowFirst;
    }

    public void setPeriodicity(long periodicity) {
        this.periodicity = periodicity;
    }

    public long getPeriodicity() {
        return this.periodicity;
    }

    public boolean test(InstrumentMessage msg) {
        if (msg == null) {
            return false;
        }

        if (lastMessageTime == Long.MIN_VALUE) {
            lastMessageTime = msg.getTimeStampMs();
            return allowFirst;
        } else if (msg.getTimeStampMs() - lastMessageTime > periodicity) {
            lastMessageTime = msg.getTimeStampMs();
            return true;
        }

        return false;
    }

    public void refresh() {
        lastMessageTime = Long.MIN_VALUE;
    }
}
