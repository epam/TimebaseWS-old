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
package com.epam.deltix.grafana.finance;

import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.computations.data.MutableGenericRecordImpl;
import com.epam.deltix.computations.data.base.*;
import com.epam.deltix.computations.data.numeric.MutableDoubleValue;
import com.epam.deltix.grafana.base.Aggregation;
import com.epam.deltix.grafana.base.annotations.*;
import com.epam.deltix.grafana.data.NumericField;
import com.epam.deltix.grafana.model.fields.Field;
import rtmath.finanalysis.indicators.ATR;

import java.util.Collection;
import java.util.List;

@GrafanaFunction(
        name = "atr", group = "finance",
        fieldArguments = {
                @FieldArgument(name = AverageTrueRange.HIGH, types = {GrafanaValueType.NUMERIC}),
                @FieldArgument(name = AverageTrueRange.LOW, types = {GrafanaValueType.NUMERIC}),
                @FieldArgument(name = AverageTrueRange.CLOSE, types = {GrafanaValueType.NUMERIC})
        },
        constantArguments = {
                @ConstantArgument(name = AverageTrueRange.PERIOD, type = ArgumentType.INT32, min = "1", defaultValue = "14")
        },
        returnFields = {@ReturnField(ValueType.DOUBLE)}
)
public class AverageTrueRange implements Aggregation {

    public static final String HIGH = "high";
    public static final String LOW = "low";
    public static final String CLOSE = "close";
    public static final String PERIOD = "period";

    private final ATR atr;
    private final String high;
    private final String low;
    private final String close;
    private final List<Field> resultFields = new ObjectArrayList<>();
    private final MutableGenericRecord resultRecord = new MutableGenericRecordImpl();
    private final MutableDoubleValue doubleValue = new MutableDoubleValue();

    public AverageTrueRange(int period, String high, String low, String close, String resultName) {
        this.atr = new ATR(period);
        this.high = high;
        this.low = low;
        this.close = close;
        Field resultField = new NumericField(resultName);
        resultFields.add(resultField);
        resultRecord.set(resultField.name(), doubleValue);
    }

    public AverageTrueRange(Arguments arguments) {
        this(arguments.getInt(PERIOD), arguments.getString(HIGH), arguments.getString(LOW), arguments.getString(CLOSE), arguments.getResultField());
    }

    @Override
    public Collection<Field> fields() {
        return resultFields;
    }

    @Override
    public boolean add(GenericRecord record) {
        if (record.containsNonNull(high) && record.containsNonNull(low) && record.containsNonNull(close)) {
            atr.add(GenericValueInfo.DOUBLE_NULL, record.getValue(high).doubleValue(), record.getValue(low).doubleValue(),
                    record.getValue(close).doubleValue(), GenericValueInfo.DOUBLE_NULL, record.timestamp());
            doubleValue.set(atr.atr);
            resultRecord.setTimestamp(record.timestamp());
            return true;
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
