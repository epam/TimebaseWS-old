package com.epam.deltix.tbwg.webapp.model.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Schema field definition.
 */
public class FieldDef {

    public FieldDef() {
    }

    public FieldDef(String name, String title, String type, boolean nullable) {
        this.name = name;
        this.type = type;
        this.title = title;
        this.nullable = nullable;
    }

    /**
     * Default visibility state.
     */
    @JsonProperty("hide")
    public boolean hidden = false;

    /**
     * Field Name.
     */
    @JsonProperty
    public String name;

    /**
     * Field Title.
     */
    @JsonProperty
    public String title;

    /**
     * Field Data Type.
     */
    @JsonProperty
    public String type;

    /**
     * Indicates that field is nullable.
     */
    @JsonProperty
    public boolean nullable;
}
