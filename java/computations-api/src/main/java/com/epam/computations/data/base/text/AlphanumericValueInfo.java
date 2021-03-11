package com.epam.deltix.computations.data.base.text;

import com.epam.deltix.anvil.util.annotation.Alphanumeric;
import com.epam.deltix.anvil.util.codec.AlphanumericCodec;
import com.epam.deltix.computations.data.base.GenericValueInfo;

public interface AlphanumericValueInfo extends GenericValueInfo {

    @Alphanumeric
    long alphanumericValue();

    @Alphanumeric
    @Override
    default long longValue() {
        return alphanumericValue();
    }

    @Override
    default String value() {
        return AlphanumericCodec.decode(longValue());
    }

    @Override
    default boolean isText() {
        return true;
    }

    @Override
    default boolean isNull() {
        return alphanumericValue() == LONG_NULL;
    }

}
