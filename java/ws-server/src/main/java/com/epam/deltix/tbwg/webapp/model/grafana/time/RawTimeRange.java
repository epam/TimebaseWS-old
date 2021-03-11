package com.epam.deltix.tbwg.webapp.model.grafana.time;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.webcohesion.enunciate.metadata.DocumentationExample;

public class RawTimeRange {

    @DocumentationExample("now-6h")
    @JsonProperty
    public String from;

    @DocumentationExample("now")
    @JsonProperty
    public String to;

}
