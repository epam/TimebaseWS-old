package com.epam.deltix.tbwg.webapp.services.charting.datasource;

import com.epam.deltix.qsrv.hf.pub.MappingTypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.tbwg.messages.SecurityStatusMessage;

public class MarketDataTypeLoader {
    public static final String SECURITY_STATUS_CLASS = "deltix.timebase.api.messages.status.SecurityStatusMessage";

    public static final MappingTypeLoader TYPE_LOADER = new MappingTypeLoader(TypeLoaderImpl.SILENT_INSTANCE);

    static {
        TYPE_LOADER.bind(SECURITY_STATUS_CLASS, SecurityStatusMessage.class);
    }

}
