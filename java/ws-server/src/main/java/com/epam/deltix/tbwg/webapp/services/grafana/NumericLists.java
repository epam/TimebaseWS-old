/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
