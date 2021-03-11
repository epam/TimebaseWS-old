package com.epam.deltix.tbwg.webapp.services.charting.provider;

import com.epam.deltix.tbwg.webapp.services.charting.datasource.MessageSource;
import com.epam.deltix.tbwg.webapp.services.charting.queries.LinesQueryResult;

import java.util.List;

public interface TransformationService {

    List<LinesQueryResult>          buildTransformationsPlan(List<MessageSource> sources);

}
