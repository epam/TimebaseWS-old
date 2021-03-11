package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.util.collections.generated.*;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public interface Aggregation {

    default Double aggregate(@Nonnull List<?> values) {
        if (values.isEmpty()) {
            return null;
        } else if (values instanceof ByteArrayList) {
            return aggregate((ByteArrayList) values);
        } else if (values instanceof ShortArrayList) {
            return aggregate((ShortArrayList) values);
        } else if (values instanceof IntegerArrayList) {
            return aggregate((IntegerArrayList) values);
        } else if (values instanceof LongArrayList) {
            return aggregate((LongArrayList) values);
        } else if (values instanceof FloatArrayList) {
            return aggregate((FloatArrayList) values);
        } else if (values instanceof DoubleArrayList) {
            return aggregate((DoubleArrayList) values);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    double aggregate(@NotEmpty @Nonnull ByteArrayList values);

    double aggregate(@NotEmpty @Nonnull ShortArrayList values);

    double aggregate(@NotEmpty @Nonnull IntegerArrayList values);

    double aggregate(@NotEmpty @Nonnull LongArrayList values);

    double aggregate(@NotEmpty @Nonnull FloatArrayList values);

    double aggregate(@NotEmpty @Nonnull DoubleArrayList values);

    String getName();

    String getAs();

}
