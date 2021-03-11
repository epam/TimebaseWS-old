package com.epam.deltix.tbwg.webapp.model.schema;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Schema static field definition.
 */
public class StaticFieldDef extends FieldDef {

    public StaticFieldDef(String name, String title, String type, boolean nullable, String value) {
        super(name, title, type, nullable);
        this.value = value;
        this.hidden = true;
    }

    /**
     * Static value for the field
     */
    @JsonProperty
    public String       value;

    /**
     * Static fields indicator
     */
    @JsonProperty(value = "static")
    public boolean      isStatic = true;
}
