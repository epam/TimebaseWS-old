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

import com.epam.deltix.computations.data.base.ArgumentType;
import com.epam.deltix.computations.data.base.Arguments;
import com.epam.deltix.computations.data.base.ValueType;
import com.epam.deltix.grafana.base.annotations.*;

@GrafanaFunction(
        name = "momentum", group = "statistics",
        fieldArguments = {@FieldArgument(name = MomentumAggregation.FIELD, types = {GrafanaValueType.NUMERIC})},
        constantArguments = {@ConstantArgument(name = MomentumAggregation.PERIOD, type = ArgumentType.INT32, defaultValue = "100", min = "1")},
        returnFields = {@ReturnField(ValueType.DOUBLE)},
        doc = "Momentum function on count period."
)
public class CountMomentum extends MomentumAggregation {

    public CountMomentum(Arguments arguments) {
        super(arguments.getString(FIELD), arguments.getInt(PERIOD), arguments.getResultField());
    }

}
