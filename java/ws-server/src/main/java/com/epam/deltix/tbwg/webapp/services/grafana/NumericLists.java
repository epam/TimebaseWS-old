package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.util.collections.generated.*;

import javax.annotation.Nonnull;
import java.util.List;

class NumericLists {

    static NumericListDelegate byteList() {
        return new ByteListDelegate();
    }

    static NumericListDelegate shortList() {
        return new ShortListDelegate();
    }

    static NumericListDelegate intList() {
        return new IntegerListDelegate();
    }

    static NumericListDelegate longList() {
        return new LongListDelegate();
    }

    static NumericListDelegate floatList() {
        return new FloatListDelegate();
    }

    static NumericListDelegate doubleList() {
        return new DoubleListDelegate();
    }

    static class ByteListDelegate implements NumericListDelegate {

        final ByteArrayList list = new ByteArrayList();

        @Override
        public void addByte(byte value) {
            list.add(value);
        }

        @Override
        public @Nonnull List<?> getList() {
            return list;
        }
    }

    static class ShortListDelegate implements NumericListDelegate {

        final ShortArrayList list = new ShortArrayList();

        @Override
        public void addShort(short value) {
            list.add(value);
        }

        @Override
        public @Nonnull List<?> getList() {
            return list;
        }
    }

    static class IntegerListDelegate implements NumericListDelegate {

        final IntegerArrayList list = new IntegerArrayList();

        @Override
        public void addInt(int value) {
            list.add(value);
        }

        @Override
        public @Nonnull List<?> getList() {
            return list;
        }
    }

    static class LongListDelegate implements NumericListDelegate {

        final LongArrayList list = new LongArrayList();

        @Override
        public void addLong(long value) {
            list.add(value);
        }

        @Override
        public @Nonnull List<?> getList() {
            return list;
        }
    }

    static class FloatListDelegate implements NumericListDelegate {

        final FloatArrayList list = new FloatArrayList();

        @Override
        public void addFloat(float value) {
            list.add(value);
        }

        @Override
        public @Nonnull List<?> getList() {
            return list;
        }
    }

    static class DoubleListDelegate implements NumericListDelegate {

        final DoubleArrayList list = new DoubleArrayList();

        @Override
        public void addDouble(double value) {
            list.add(value);
        }

        @Override
        public @Nonnull List<?> getList() {
            return list;
        }
    }

}
