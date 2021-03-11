package com.epam.deltix.computations.data.base.time;

import com.epam.deltix.anvil.util.annotation.Timestamp;
import com.epam.deltix.computations.data.base.MutableGenericValueInfo;

public interface MutableTimestampValueInfo extends TimestampValueInfo, MutableGenericValueInfo {

    @Override
    void setTimestamp(@Timestamp long value);

    @Override
    default void setNull() {
        setTimestamp(TIMESTAMP_NULL);
    }
}
