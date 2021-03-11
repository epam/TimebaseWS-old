package com.epam.deltix.tbwg.webapp.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.NotNull;
import com.epam.deltix.tbwg.webapp.model.filter.FilterType;

import java.util.List;

/**
 * @author Daniil Yarmalkevich
 * Date: 6/24/2019
 */
public class RawFilter {


    @JsonProperty
    @NotNull
    public FilterType type;

    @JsonProperty
    public List<String> data;
}
