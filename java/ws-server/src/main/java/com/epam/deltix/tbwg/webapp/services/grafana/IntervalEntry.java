package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.anvil.util.Reusable;
import com.epam.deltix.util.collections.generated.ObjectToDoubleHashMap;

public class IntervalEntry implements Reusable {

    private final ObjectToDoubleHashMap<String> values = new ObjectToDoubleHashMap<>();

    private long timestamp = Long.MIN_VALUE;
    @Override
    public void reuse() {
        values.clear();
        timestamp = Long.MIN_VALUE;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ObjectToDoubleHashMap<String> getValues() {
        return values;
    }

    public void put(String field, double value) {
        values.put(field, value);
    }
}
