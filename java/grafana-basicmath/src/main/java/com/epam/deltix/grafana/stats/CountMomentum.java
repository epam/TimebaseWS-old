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
