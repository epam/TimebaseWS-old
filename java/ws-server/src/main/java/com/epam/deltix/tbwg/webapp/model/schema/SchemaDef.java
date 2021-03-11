package com.epam.deltix.tbwg.webapp.model.schema;

/**
 * Stream schema definition
 */
public class SchemaDef {

    /**
     * Schema top-types list (used to represent messages)
     */
    public TypeDef[]   types;

    /**
     * Schema all-types list (including enumeration and nested types)
     */
    public TypeDef[]   all;
}
