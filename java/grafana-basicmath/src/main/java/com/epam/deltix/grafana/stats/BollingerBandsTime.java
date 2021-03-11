package com.epam.deltix.grafana.stats;

import com.epam.deltix.computations.data.base.ArgumentType;
import com.epam.deltix.computations.data.base.Arguments;
import com.epam.deltix.computations.data.base.ValueType;
import com.epam.deltix.grafana.base.annotations.*;

@GrafanaFunction(
        name = "bollinger", group = "statistics",
        fieldArguments = {@FieldArgument(name = BollingerBands.FIELD, types = {GrafanaValueType.NUMERIC})},
        constantArguments = {
                @ConstantArgument(name = BollingerBands.PERIOD, type = ArgumentType.INT64, defaultValue = "1000", min = "1"),
                @ConstantArgument(name = BollingerBands.FACTOR, type = ArgumentType.FLOAT64, defaultValue = "1")
        },
        returnFields = {
                @ReturnField(constantName = BollingerBands.LOWER_BAND, value = ValueType.DOUBLE),
                @ReturnField(constantName = BollingerBands.MIDDLE_BAND, value = ValueType.DOUBLE),
                @ReturnField(constantName = BollingerBands.UPPER_BAND, value = ValueType.DOUBLE),
                @ReturnField(constantName = BollingerBands.PERCENT_B, value = ValueType.DOUBLE),
                @ReturnField(constantName = BollingerBands.BAND_WIDTH, value = ValueType.DOUBLE)
        }
)
public class BollingerBandsTime extends BollingerBands {

    public BollingerBandsTime(Arguments arguments) {
        super(arguments.getString(FIELD), arguments.getLong(PERIOD), arguments.getDouble(FACTOR),
                arguments.getString(LOWER_BAND, LOWER_BAND),
                arguments.getString(MIDDLE_BAND, MIDDLE_BAND),
                arguments.getString(UPPER_BAND, UPPER_BAND),
                arguments.getString(PERCENT_B, PERCENT_B),
                arguments.getString(BAND_WIDTH, BAND_WIDTH));
    }

}
