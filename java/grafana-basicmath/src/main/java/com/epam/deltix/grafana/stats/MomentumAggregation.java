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
import com.epam.deltix.computations.data.base.GenericRecord;
import com.epam.deltix.computations.data.base.MutableGenericRecord;
import com.epam.deltix.computations.data.numeric.MutableDoubleValue;
import com.epam.deltix.grafana.base.Aggregation;
import com.epam.deltix.grafana.data.NumericField;
import com.epam.deltix.grafana.model.fields.Field;
import rtmath.finanalysis.indicators.Momentum;

import java.util.Collection;
import java.util.List;

public class MomentumAggregation implements Aggregation {

    public static final String FIELD = "field";
    public static final String PERIOD = "period";
    public static final String TIME_PERIOD = "timePeriod";

    private final String fieldName;
    private final Momentum momentum;

    private final List<Field> resultFields = new ObjectArrayList<>();
    private final MutableGenericRecord resultRecord = new MutableGenericRecordImpl();
    private final MutableDoubleValue doubleValue = new MutableDoubleValue();

    public MomentumAggregation(String fieldName, int period, String resultName) {
        this.fieldName = fieldName;
        this.momentum = new Momentum(period);
        Field resultField = new NumericField(resultName);
        resultFields.add(resultField);
        resultRecord.set(resultField.name(), doubleValue);
    }

    public MomentumAggregation(String fieldName, long timePeriod, String resultName) {
        this.fieldName = fieldName;
        this.momentum = new Momentum(timePeriod);
        Field resultField = new NumericField(resultName);
        resultFields.add(resultField);
        resultRecord.set(resultField.name(), doubleValue);
    }

    @Override
    public Collection<Field> fields() {
        return resultFields;
    }

    @Override
    public boolean add(GenericRecord record) {
        if (record.containsNonNull(fieldName)) {
            momentum.add(record.getValue(fieldName).doubleValue(), record.timestamp());
            if (!Double.isNaN(momentum.value)) {
                doubleValue.set(momentum.value);
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
