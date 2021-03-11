package com.epam.deltix.grafana.stats;

import com.epam.deltix.computations.data.base.ArgumentType;
import com.epam.deltix.computations.data.base.Arguments;
import com.epam.deltix.computations.data.base.ValueType;
import com.epam.deltix.grafana.base.annotations.*;

@GrafanaFunction(
        name = "momentum", group = "statistics",
        fieldArguments = {@FieldArgument(name = MomentumAggregation.FIELD, types = {GrafanaValueType.NUMERIC})},
        constantArguments = {@ConstantArgument(name = MomentumAggregation.TIME_PERIOD, type = ArgumentType.INT64, defaultValue = "1000", min = "1")},
        returnFields = {@ReturnField(ValueType.DOUBLE)},
        doc = "Momentum function on time period."
)
public class TimeMomentum extends MomentumAggregation {

    public TimeMomentum(Arguments arguments) {
        super(arguments.getString(FIELD), arguments.getLong(TIME_PERIOD), arguments.getResultField());
    }

}
