package com.epam.deltix.tbwg.webapp.services.grafana.base;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.tbwg.webapp.model.grafana.DynamicList;
import com.epam.deltix.tbwg.webapp.model.grafana.TimeSeriesEntry;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.DataQueryRequest;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.SelectQuery;
import com.epam.deltix.tbwg.webapp.model.grafana.time.TimeRange;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.NoSuchStreamException;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.ValidationException;
import com.epam.deltix.tbwg.webapp.utils.grafana.GrafanaUtils;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.computations.base.exc.RecordValidationException;
import com.epam.deltix.grafana.model.DataFrame;

import java.util.List;
import java.util.stream.Collectors;

public interface GrafanaServiceNew {

    Log LOG = LogFactory.getLog(GrafanaServiceNew.class);

    DataFrame dataFrame(SelectQuery query, TimeRange timeRange, int maxDataPoints, Long intervalMs) throws ValidationException,
            RecordValidationException;

    default List<DataFrame> dataFrames(DataQueryRequest<SelectQuery> request) throws RecordValidationException,
            ValidationException {
        List<DataFrame> dataFrames = new ObjectArrayList<>();
        for (SelectQuery target : request.getTargets()) {
            dataFrames.add(dataFrame(target, request.getRange(), request.getMaxDataPoints(), request.getIntervalMs()));
        }
        return dataFrames;
    }

    default List<TimeSeriesEntry> timeSeries(DataQueryRequest<SelectQuery> request) throws RecordValidationException,
            ValidationException {
        return dataFrames(request).stream().flatMap(df -> GrafanaUtils.convert(df).stream()).collect(Collectors.toList());
    }

    default List<Object> select(DataQueryRequest<SelectQuery> request) throws RecordValidationException,
            ValidationException {
        long start = System.currentTimeMillis();
        List<SelectQuery> targets = request.getTargets();
        request.setTargets(targets.stream().filter(q -> q.getView() == null || q.getView() == SelectQuery.View.DATAFRAME).collect(Collectors.toList()));
        List<Object> list = new ObjectArrayList<>();
        list.addAll(dataFrames(request));
        request.setTargets(targets.stream().filter(q -> q.getView() == SelectQuery.View.TIMESERIES).collect(Collectors.toList()));
        list.addAll(timeSeries(request));
        long end = System.currentTimeMillis();
        LOG.info().append("Request execution took ").append((end - start) / 1000., 3).append(" seconds.").commit();
        return list;
    }

    List<String> groupByViewOptions();

    DynamicList listStreams(String template, int offset, int limit);

    DynamicList listSymbols(String streamKey, String template, int offset, int limit) throws NoSuchStreamException;

}
