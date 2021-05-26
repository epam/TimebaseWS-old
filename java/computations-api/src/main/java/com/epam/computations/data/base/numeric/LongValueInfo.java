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
package com.epam.deltix.computations.data.base.numeric;

import com.epam.deltix.dfp.Decimal64Utils;

public interface LongValueInfo extends NumberValueInfo {

    long longValue();

    @Override
    default Long value() {
        return isNull() ? null: longValue();
    }

    @Override
    default float floatValue() {
        return isNull() ? FLOAT_NULL: longValue();
    }

    @Override
    default double doubleValue() {
        return isNull() ? DOUBLE_NULL: longValue();
    }

    @Override
    default long decimalValue() {
        return isNull() ? DECIMAL_NULL: Decimal64Utils.fromLong(longValue());
    }

    @Override
    default boolean isNull() {
        return longValue() == LONG_NULL;
    }
}
