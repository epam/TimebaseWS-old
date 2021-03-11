package com.epam.deltix.tbwg.webapp.model.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Schema type description.
 */
public class TypeDef {

    public TypeDef() {
    }

    public TypeDef(String name, String title, FieldDef[] fields) {
        this.name = name != null ? name : "";
        this.title = title;
        this.fields = fields;
    }

    @JsonProperty
    public boolean      isEnum = false;

    @JsonProperty
    public String       name;

    @JsonProperty
    public String       title;

    /*
     *  list of fields
     */
    @JsonProperty
    public FieldDef[]   fields;

    /*
     *   Name of the parent TypeDef
     */
    @JsonProperty
    public String       parent;

    /*
     *   Set column visible if found
     */
    public void         setVisible(String fieldName) {
        Optional<FieldDef> first = Stream.of(fields).filter(x -> fieldName.equals(x.name)).findFirst();
        first.ifPresent(fieldDef -> fieldDef.hidden = false);
    }
}
