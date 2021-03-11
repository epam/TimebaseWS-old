package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.tbwg.webapp.model.grafana.time.TimeRange;
import com.epam.deltix.timebase.messages.InstrumentMessage;

public interface Aggregator {

    default IntervalEntry nextInterval(MessageSource<InstrumentMessage> messageSource) {
        IntervalEntry intervalEntry = new IntervalEntry();
        if (nextInterval(messageSource, intervalEntry)) {
            return intervalEntry;
        } else {
            return null;
        }
    }

    boolean nextInterval(MessageSource<InstrumentMessage> messageSource, IntervalEntry intervalEntry);

    static long calculateStep(long startTime, long endTime, long intervals) {
        long d = intervals - (endTime - startTime) % intervals;
        return (endTime - startTime + d) / intervals;
    }

    static long calculateStep(TimeRange range, long intervals) {
        return calculateStep(range.getFrom().toEpochMilli(), range.getTo().toEpochMilli(), intervals);
    }
}
