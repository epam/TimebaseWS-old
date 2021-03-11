package com.epam.deltix.computations.data.time;

import com.epam.deltix.anvil.util.annotation.Timestamp;
import com.epam.deltix.computations.data.base.time.MutableTimestampValueInfo;

public class MutableTimestampValue implements MutableTimestampValueInfo {

    @Timestamp
    private long value;

    public MutableTimestampValue(@Timestamp long value) {
        this.value = value;
    }

    public MutableTimestampValue() {
        this(TIMESTAMP_NULL);
    }

    @Timestamp
    @Override
    public long timestampValue() {
        return value;
    }

    @Override
    public void reuse() {
        value = TIMESTAMP_NULL;
    }

    @Override
    public void setTimestamp(@Timestamp long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + value();
    }
}
