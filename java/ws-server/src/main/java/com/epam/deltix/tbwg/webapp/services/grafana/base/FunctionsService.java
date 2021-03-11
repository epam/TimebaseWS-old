package com.epam.deltix.tbwg.webapp.services.grafana.base;

import com.epam.deltix.tbwg.webapp.model.grafana.aggs.GrafanaFunctionDef;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.SelectQuery;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.ValidationException;
import com.epam.deltix.grafana.base.Aggregation;

import java.util.List;

public interface FunctionsService {

    List<GrafanaFunctionDef> listFunctions(String key);

    Aggregation aggregation(SelectQuery selectQuery, long start, long end, long interval, List<SelectQuery.TimebaseField> groupBy,
                            List<String> symbols) throws ValidationException;

}
