package com.epam.deltix.tbwg.webapp.services.charting.datasource;

import com.epam.deltix.tbwg.webapp.services.charting.queries.LinesQuery;

import java.util.List;

public interface DataSource {

    List<MessageSource>         buildSources(List<LinesQuery> queries);

}
