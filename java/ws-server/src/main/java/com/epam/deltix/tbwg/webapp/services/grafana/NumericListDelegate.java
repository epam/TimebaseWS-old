package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;

import javax.annotation.Nonnull;
import java.util.List;

interface NumericListDelegate {

    @Nonnull List<?> getList();

    default void addBoolean(boolean value) {
        addByte((byte) (value ? 1: 0));
    }

    default void addByte(byte value) {
        addShort(value);
    }

    default void addShort(short value) {
        addInt(value);
    }

    default void addInt(int value) {
        addLong(value);
    }

    default void addLong(long value) {
        throw new UnsupportedOperationException();
    }

    default void addFloat(float value) {
        addDouble(value);
    }

    default void addDouble(double value) {
        throw new UnsupportedOperationException();
    }

    default void addDecimal(@Decimal long value) {
        addDouble(Decimal64Utils.toDouble(value));
    }

    default void clear() {
        getList().clear();
    }

}
