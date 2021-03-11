package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiAggregator implements Aggregator {

    private final long startTime;
    private final long endTime;
    private final long step;
    private final Map<String, List<Aggregation>> aggregations;

    private final NumericDecoder decoder = new NumericDecoder();
    private final Map<String, NumericListDelegate> rawValues = new HashMap<>();

    private long currentFirst = Long.MIN_VALUE;
    private long currentLast = Long.MIN_VALUE;

    private boolean cursorFinished = false;

    public MultiAggregator(long startTime, long endTime, long step, Map<String, List<Aggregation>> aggregations) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.aggregations = aggregations;
        this.step = step;
    }

    private boolean next() {
        if (cursorFinished) {
            return false;
        }
        if (currentFirst == Long.MIN_VALUE) {
            currentFirst = startTime;
        } else {
            currentFirst += step;
        }
        if (currentFirst > endTime) {
            return false;
        }
        currentLast = Math.min(currentFirst + step, endTime);
        return true;
    }

    @Override
    public boolean nextInterval(MessageSource<InstrumentMessage> messageSource, IntervalEntry intervalEntry) {
        intervalEntry.reuse();
        if (next()) {
            intervalEntry.setTimestamp(currentLast);
            rawValues.clear();
            do {
                if (messageSource.getMessage() != null && messageSource.getMessage().getTimeStampMs() != Long.MIN_VALUE) {
                    RawMessage rawMessage = (RawMessage) messageSource.getMessage();
                    if (rawMessage.getTimeStampMs() >= currentFirst && rawMessage.getTimeStampMs() < currentLast) {
                        decoder.decode(rawMessage, rawValues);
                    } else if (rawMessage.getTimeStampMs() >= currentLast) {
                        break;
                    }
                }
                if (!messageSource.next()) {
                    cursorFinished = true;
                }
            } while (!cursorFinished && messageSource.getMessage().getTimeStampMs() < currentLast);
            rawValues.forEach((field, values) -> {
                if (!values.getList().isEmpty()) {
                    List<Aggregation> aggs = aggregations.get(field);
                    for (Aggregation aggregation : aggs) {
                        intervalEntry.put(fieldName(field, aggregation), aggregation.aggregate(values.getList()));
                    }
                }
            });
            return true;
        } else {
            return false;
        }
    }

    private static String fieldName(String field, Aggregation aggregation) {
        return aggregation.getAs() == null ? String.format("%s(%s)", aggregation.getName(), field): aggregation.getAs();
    }
}
