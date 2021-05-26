/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.tbwg.webapp.model.charting.TimeSeriesEntry;
import com.epam.deltix.tbwg.webapp.model.grafana.AggregationType;
import com.epam.deltix.tbwg.webapp.model.grafana.GrafanaChartType;
import com.epam.deltix.tbwg.webapp.model.grafana.StreamInfo;
import com.epam.deltix.tbwg.webapp.model.grafana.StreamSchema;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.BarsQuery;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.DataQueryRequest;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.PricesL2Query;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.SelectQuery;
import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.NoSuchStreamException;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.ValidationException;
import com.epam.deltix.grafana.model.DataFrame;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Daniil Yarmalkevich
 * Date: 8/22/2019
 */
public interface GrafanaService {

    Collection<String> listStreams(String template, int limit);

    Collection<String> listSymbols(String streamKey, String template, int limit) throws NoSuchStreamException;

    StreamSchema schema(String streamKey) throws NoSuchStreamException;

    Collection<String> aggregations();

    StreamInfo streamInfo(String streamKey);

    List<TimeSeriesEntry> getTimeSeries(
            @Nullable String stream,
            @Nonnull List<String> symbols,
            @Nonnull GrafanaChartType type,
            @Nonnull TimeInterval interval,
            int maxPoints,
            int levels,
            @Nullable Map<String, List<String>> fields,
            @Nullable AggregationType aggregationType
            );

    List<TimeSeriesEntry> getPriceL2(@Nonnull PricesL2Query query, TimeInterval timeInterval, int maxDataPoints);

    List<TimeSeriesEntry> getBars(@Nonnull BarsQuery query, TimeInterval timeInterval, int maxDataPoints);

    List<TimeSeriesEntry> select(DataQueryRequest<SelectQuery> request) throws ValidationException;

    List<DataFrame> selectDataFrames(DataQueryRequest<SelectQuery> request) throws ValidationException;

}
