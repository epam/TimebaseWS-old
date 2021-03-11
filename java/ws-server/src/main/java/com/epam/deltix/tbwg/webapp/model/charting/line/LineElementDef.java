package com.epam.deltix.tbwg.webapp.model.charting.line;

import java.util.Objects;

/**
 * A definition of a base line point.
 * @label BaseLinePoint
 */
public abstract class LineElementDef implements LineElement {
    private long time;

    public LineElementDef() {
    }

    /**
     * The timestamp. X axis value for a line point.
     */
    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineElementDef)) return false;
        LineElementDef that = (LineElementDef) o;
        return Objects.equals(getTime(), that.getTime());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getTime());
    }
}
