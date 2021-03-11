package com.epam.deltix.tbwg.webapp.model.charting.line;

import java.time.Instant;

/**
 * A definition of a line element.
 * @label LineElement
 */
public interface LineElement {
    /**
     * The timestamp. X axis value for a line point.
     */
    long getTime();
}
