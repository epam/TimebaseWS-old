package com.epam.deltix.computations.data.base.time;

import com.epam.deltix.computations.data.base.MutableGenericValueInfo;
import com.epam.deltix.computations.data.base.annotations.TimeOfDay;

public interface MutableTimeOfDayValueInfo extends TimeOfDayValueInfo, MutableGenericValueInfo {

    @Override
    void setTimeOfDay(@TimeOfDay int value);

    @Override
    default void setNull() {
        setTimeOfDay(TIME_OF_DAY_NULL);
    }
}
