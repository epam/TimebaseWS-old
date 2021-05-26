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
