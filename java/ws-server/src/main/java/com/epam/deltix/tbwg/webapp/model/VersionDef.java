package com.epam.deltix.tbwg.webapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * API Version information.
 */
public class VersionDef {

    private static String VERSION = VersionDef.class.getPackage().getImplementationVersion();

    /**
     * Name
     */
    @JsonProperty
    public String   name = "Timebase Web Gateway";

    /**
     * Current version
     */
    @JsonProperty
    public String   version = VERSION;

    /**
     * Current time
     */
    @JsonProperty
    public long     timestamp = System.currentTimeMillis();

    /**
     * Timebase client version
     */
    @JsonProperty
    public String   timebase = deltix.qsrv.hf.tickdb.client.Version.getVersion();

    @JsonProperty
    public boolean  authentication = true;

}
