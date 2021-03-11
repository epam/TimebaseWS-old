package com.epam.deltix.tbwg.webapp.services.charting;

import java.time.Duration;
import java.time.Instant;

public class TimeInterval {
    private final Instant startTime;
    private final Instant endTime;

    public TimeInterval(Instant startTime, Instant endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public TimeInterval(long startTime, long endTime) {
        this.startTime = Instant.ofEpochMilli(startTime);
        this.endTime = Instant.ofEpochMilli(endTime);
    }

    public Instant getStartTime() {
        return startTime;
    }

    public TimeInterval setStartTime(Instant startTime) {
        return new TimeInterval(startTime, this.endTime);
    }

    public Instant getEndTime() {
        return endTime;
    }

    public TimeInterval setEndTime(Instant endTime) {
        return new TimeInterval(this.startTime, endTime);
    }

    public long getStartTimeMilli() {
        return startTime.toEpochMilli();
    }

    public TimeInterval setStartTimeMilli(long startTime) {
        return setStartTime(Instant.ofEpochMilli(startTime));
    }

    public long getEndTimeMilli() {
        return endTime.toEpochMilli();
    }

    public TimeInterval setEndTimeMilli(long endTime) {
        return setEndTime(Instant.ofEpochMilli(endTime));
    }

    public boolean isEmpty() {
        return startTime.isAfter(endTime);
    }

    public Duration getDuration() {
        return isEmpty() ? Duration.ZERO : Duration.between(startTime, endTime);
    }

    public TimeInterval union(final TimeInterval that) {
        if (getStartTime().isAfter(that.getStartTime())) {
            if (getEndTime().isBefore(that.getEndTime())) {
                return that;
            } else {
                return setStartTime(that.getStartTime());
            }
        } else if (getEndTime().isBefore(that.getEndTime())) {
            return setEndTime(that.getEndTime());
        }
        return this;
    }

    public TimeInterval intersection(final TimeInterval that) {
        if (getStartTime().isBefore(that.getStartTime())) {
            if (getEndTime().isAfter(that.getEndTime())) {
                return that;
            } else {
                return setStartTime(that.getStartTime());
            }
        } else if (getEndTime().isAfter(that.getEndTime())) {
            return setEndTime(that.getEndTime());
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeInterval)) return false;

        TimeInterval interval = (TimeInterval) o;

        if (!getStartTime().equals(interval.getStartTime())) return false;
        return getEndTime().equals(interval.getEndTime());
    }

    @Override
    public int hashCode() {
        int result = getStartTime().hashCode();
        result = 31 * result + getEndTime().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return startTime + " - " + endTime;
    }
}
