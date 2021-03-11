package com.epam.deltix.computations.data.base.text;

import com.epam.deltix.anvil.util.annotation.Alphanumeric;
import com.epam.deltix.anvil.util.codec.AlphanumericCodec;
import com.epam.deltix.computations.data.base.MutableGenericValueInfo;

public interface MutableAlphanumericValueInfo extends AlphanumericValueInfo, MutableGenericValueInfo {

    @Override
    void setAlphanumeric(@Alphanumeric long value);

    @Override
    default void set(CharSequence value) {
        setAlphanumeric(AlphanumericCodec.encode(value));
    }

    @Override
    default void setNull() {
        setAlphanumeric(ALPHANUMERIC_NULL);
    }
}
