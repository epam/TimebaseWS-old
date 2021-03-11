package com.epam.deltix.tbwg.webapp.services.grafana.exc;

import com.epam.deltix.computations.data.base.ArgumentType;

public class ConstantValidationException extends ValidationException {

    private final ArgumentType type;
    private final Object value;
    private final Object min;
    private final Object max;

    public ConstantValidationException(ArgumentType type, String value, String min, String max) {
        this.type = type;
        this.value = value;
        this.min = min;
        this.max = max;
    }

    @Override
    public String getMessage() {
        return String.format("Value '%s' of type '%s' does not meet requirements min='%s', max='%s'.", value, type, min, max);
    }
}
