package com.epam.deltix.tbwg.webapp.model.charting.line;

import java.util.Objects;

/**
 * A definition of a line point.
 * @label LinePoint
 */
public class LinePointDef extends LineElementDef {
    private String value;

    public LinePointDef() {
    }

    /**
     * Y axis value for a line point.
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LinePointDef that = (LinePointDef) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
