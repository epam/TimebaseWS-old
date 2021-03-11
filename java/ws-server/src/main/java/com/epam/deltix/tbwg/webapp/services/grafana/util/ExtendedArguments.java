package com.epam.deltix.tbwg.webapp.services.grafana.util;

import com.epam.deltix.tbwg.webapp.services.grafana.exc.ConstantParseException;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.ConstantValidationException;
import com.epam.deltix.computations.data.base.MutableArguments;
import com.epam.deltix.grafana.base.annotations.AnnotationUtil;
import com.epam.deltix.grafana.base.annotations.ConstantArgument;

public interface ExtendedArguments extends MutableArguments {

    default void set(ConstantArgument argument, String value) throws ConstantValidationException, ConstantParseException {
        try {
            if (AnnotationUtil.create(argument).test(value)) {
                set(argument.name(), value, argument.type());
            }
        } catch (NumberFormatException exc) {
            throw new ConstantParseException(argument, value);
        }
    }
}