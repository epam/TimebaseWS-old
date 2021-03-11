package com.epam.deltix.tbwg.webapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error response.
 */
public class ErrorDef {

    /**
     * Error Message
     */
    @JsonProperty
    public String message;

    @JsonProperty
    public String name;
}
