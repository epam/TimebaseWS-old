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
package com.epam.deltix.grafana.stats;

import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.computations.data.MutableGenericRecordImpl;
import com.epam.deltix.computations.data.base.*;
import com.epam.deltix.computations.data.numeric.MutableDoubleValue;
import com.epam.deltix.grafana.base.Aggregation;
import com.epam.deltix.grafana.base.annotations.*;
import com.epam.deltix.grafana.data.NumericField;
import com.epam.deltix.grafana.model.fields.Field;
import rtmath.finanalysis.indicators.MACD;

import java.util.Collection;
import java.util.List;

@GrafanaFunction(
        name = "macd", group = "statistics",
        fieldArguments = {@FieldArgument(name = MovingAverageConvergenceDivergence.FIELD, types = {GrafanaValueType.NUMERIC})},
        constantArguments = {
                @ConstantArgument(name = MovingAverageConvergenceDivergence.FAST_PERIOD, type = ArgumentType.INT32,
                        defaultValue = "12", min = "1"),
                @ConstantArgument(name = MovingAverageConvergenceDivergence.SLOW_PERIOD, type = ArgumentType.INT32,
                        defaultValue = "26", min = "1"),
                @ConstantArgument(name = MovingAverageConvergenceDivergence.SIGNAL_PERIOD, type = ArgumentType.INT32,
                        defaultValue = "9", min = "1")
        },
        returnFields = {
                @ReturnField(constantName = MovingAverageConvergenceDivergence.VALUE, value = ValueType.DOUBLE),
                @ReturnField(constantName = MovingAverageConvergenceDivergence.SIGNAL, value = ValueType.DOUBLE),
                @ReturnField(constantName = MovingAverageConvergenceDivergence.HISTOGRAM, value = ValueType.DOUBLE)
        },
        doc = "Simple moving average on some points count."
)
public class MovingAverageConvergenceDivergence implements Aggregation {

    public static final String FIELD = "field";
    public static final String FAST_PERIOD = "fastPeriod";
    public static final String SLOW_PERIOD = "slowPeriod";
    public static final String SIGNAL_PERIOD = "signalPeriod";
    public static final String VALUE = "value";
    public static final String SIGNAL = "signal";
    public static final String HISTOGRAM = "histogram";

    private final String fieldName;
    private final MACD macd;
    private final List<Field> resultFields = new ObjectArrayList<>();
    private final MutableGenericRecord resultRecord = new MutableGenericRecordImpl();
    private final MutableDoubleValue value = new MutableDoubleValue();
    private final MutableDoubleValue signal = new MutableDoubleValue();
    private final MutableDoubleValue histogram = new MutableDoubleValue();

    public MovingAverageConvergenceDivergence(String fieldName, int fastPeriod, int slowPeriod, int signalPeriod,
                                              String value, String signal, String histogram) {
        this.fieldName = fieldName;
        this.macd = new MACD(fastPeriod, slowPeriod, signalPeriod);

        resultFields.add(new NumericField(value));
        resultFields.add(new NumericField(signal));
        resultFields.add(new NumericField(histogram));

        resultRecord.set(value, this.value);
        resultRecord.set(signal, this.signal);
        resultRecord.set(histogram, this.histogram);
    }

    public MovingAverageConvergenceDivergence(Arguments arguments) {
        this(arguments.getString(FIELD), arguments.getInt(FAST_PERIOD, 12), arguments.getInt(SLOW_PERIOD, 26),
                arguments.getInt(SIGNAL_PERIOD, 9), arguments.getString(VALUE, VALUE), arguments.getString(SIGNAL, SIGNAL),
                arguments.getString(HISTOGRAM, HISTOGRAM));
    }

    @Override
    public Collection<Field> fields() {
        return resultFields;
    }

    @Override
    public boolean add(GenericRecord record) {
        if (record.containsNonNull(fieldName)) {
            macd.add(record.getValue(fieldName).doubleValue(), record.timestamp());
            if (macd.ready) {
                value.set(macd.value);
                signal.set(macd.signal);
                histogram.set(macd.histogram);
                resultRecord.setTimestamp(record.timestamp());
                return true;
            }
        }
        return false;
    }

    @Override
    public GenericRecord record(long timestamp) {
        return resultRecord;
    }

    @Override
    public boolean isValid(GenericRecord record) {
        return true;
    }
}
