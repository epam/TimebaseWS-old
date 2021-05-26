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

import com.epam.deltix.anvil.util.MutableInt;
import com.epam.deltix.anvil.util.StringUtil;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.tbwg.webapp.model.charting.ChartType;
import com.epam.deltix.tbwg.webapp.model.charting.TimeSeriesEntry;
import com.epam.deltix.tbwg.webapp.model.charting.line.BarElementDef;
import com.epam.deltix.tbwg.webapp.model.charting.line.LinePointDef;
import com.epam.deltix.tbwg.webapp.model.charting.line.TagElementDef;
import com.epam.deltix.tbwg.webapp.model.grafana.*;
import com.epam.deltix.tbwg.webapp.model.grafana.filters.FieldFilter;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.BarsQuery;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.DataQueryRequest;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.PricesL2Query;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.SelectQuery;
import com.epam.deltix.tbwg.webapp.model.grafana.time.TimeRange;
import com.epam.deltix.tbwg.webapp.services.TimebaseServiceImpl;
import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;
import com.epam.deltix.tbwg.webapp.services.charting.provider.LinesProvider;
import com.epam.deltix.tbwg.webapp.services.charting.queries.BookSymbolQueryImpl;
import com.epam.deltix.tbwg.webapp.services.charting.queries.ChartingResult;
import com.epam.deltix.tbwg.webapp.services.charting.queries.LinesQueryResult;
import com.epam.deltix.tbwg.webapp.services.grafana.base.FunctionsService;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.NoSuchStreamException;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.NoSuchSymbolsException;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.NoTargetsException;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.ValidationException;
import com.epam.deltix.tbwg.webapp.services.grafana.qql.SelectBuilder2;
import com.epam.deltix.tbwg.webapp.services.grafana.qql.SelectBuilder2.NoSuchTypeException;
import com.epam.deltix.tbwg.webapp.services.grafana.qql.SelectBuilder2.Type;
import com.epam.deltix.tbwg.webapp.services.grafana.qql.SelectBuilder2.WrongTypeException;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.computations.data.base.ValueType;
import com.epam.deltix.grafana.data.MutableDataFrame;
import com.epam.deltix.grafana.data.MutableDataFrameImpl;
import com.epam.deltix.grafana.model.DataFrame;
import com.epam.deltix.grafana.model.fields.Column;
import com.epam.deltix.grafana.model.fields.ColumnImpl;
import com.epam.deltix.grafana.model.fields.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import rtmath.containers.generated.CharSequenceToIntHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Daniil Yarmalkevich
 * Date: 8/22/2019
 */
@Service
@ConfigurationProperties(prefix = "grafana")
public class GrafanaServiceImpl implements GrafanaService {

    private static final Log LOG = LogFactory.getLog(GrafanaServiceImpl.class);

    private static final TypeInfo INSTRUMENT_MSG = instrumentMessage();

    private final TimebaseServiceImpl timebase;
    private final LinesProvider provider;
    private final FunctionsService functionsService;


    private Set<String> streams = new HashSet<>();
    private String include;
    private String exclude;
    private Pattern iPattern;
    private Pattern ePattern;

    @Autowired
    public GrafanaServiceImpl(TimebaseServiceImpl timebase, LinesProvider provider, FunctionsService functionsService) {
        this.timebase = timebase;
        this.provider = provider;
        this.functionsService = functionsService;
    }

    @Override
    public List<TimeSeriesEntry> getTimeSeries(
            @Nullable String stream,
            @Nonnull List<String> symbols,
            @Nonnull GrafanaChartType type,
            @Nonnull TimeInterval interval,
            int maxPoints,
            int levels,
            @Nullable Map<String, List<String>> fields,
            @Nullable AggregationType aggregationType
    ) {
        switch (type) {
            case BARS: {
                ChartingResult result = provider.getLines(Collections.singletonList(new BookSymbolQueryImpl(stream, symbols.get(0),
                        ChartType.BARS, interval, maxPoints, levels)));
                return buildGrafanaBars(result);
            }
            case PRICES_L2: {
                ChartingResult result = provider.getLines(Collections.singletonList(new BookSymbolQueryImpl(stream, symbols.get(0),
                        ChartType.PRICES_L2, interval, maxPoints, levels)));
                return buildGrafanaChart(result);
            }
            case CUSTOM: {
                if (fields == null || fields.isEmpty()) {
                    return Collections.emptyList();
                }
                return buildSimple(stream, symbols, interval, maxPoints, fields, aggregationType);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<TimeSeriesEntry> select(DataQueryRequest<SelectQuery> request) throws ValidationException {
        if (request.getTargets().isEmpty()) {
            throw new NoTargetsException();
        }

        List<TimeSeriesEntry> result = new ArrayList<>();
        for (SelectQuery query : request.getTargets()) {
            long step = calculateStep(query, request);
            result.addAll(select(query.getStream(), request.getRange(), step, query));
        }
        return result;
    }

    @Override
    public List<DataFrame> selectDataFrames(DataQueryRequest<SelectQuery> request) throws ValidationException {
        if (request.getTargets().isEmpty()) {
            throw new NoTargetsException();
        }

        List<DataFrame> result = new ObjectArrayList<>();

        for (SelectQuery query : request.getTargets()) {
            MutableDataFrame mutableDataFrame = new MutableDataFrameImpl("select query");
            long step = calculateStep(query, request);
            SelectBuilder2 selectBuilder = constructQuery(query.getStream(), request.getRange(), query);
            try (MessageSource<InstrumentMessage> messageSource = selectBuilder.executeRaw()) {
                Map<String, Column> columns = new HashMap<>();
                columns.put("timestamp", new ColumnImpl("timestamp", FieldType.TIME));
                Map<String, List<Aggregation>> aggregations = new HashMap<>();
                query.getFields().values().stream()
                        .flatMap(Collection::stream)
                        .forEach(tbField -> aggregations.put(
                                tbField.getName(),
                                tbField.getAggregations().stream()
                                        .map(AggregationInfo::getName)
                                        .map(Aggregations::fromString)
                                        .collect(Collectors.toList())
                        ));
                Aggregator aggregator = new MultiAggregator(request.getRange().getFrom().toEpochMilli(),
                        request.getRange().getTo().toEpochMilli(), step, aggregations);
                IntervalEntry entry = new IntervalEntry();
                HashSet<String> visited = new HashSet<>();
                MutableInt records = new MutableInt(0);
                while (aggregator.nextInterval(messageSource, entry)) {
                    visited.clear();
                    columns.get("timestamp").values().add(entry.getTimestamp());
                    visited.add("timestamp");
                    entry.getValues().keyIterator().forEachRemaining(key -> {
                        Column column = columns.computeIfAbsent(key, k -> {
                            Column newColumn = new ColumnImpl(k, FieldType.NUMBER);
                            for (int i = 0; i < records.get(); i++) {
                                newColumn.values().add(null);
                            }
                            return newColumn;
                        });
                        column.values().add(entry.getValues().get(key, Double.NaN));
                        visited.add(key);
                    });
                    columns.keySet().stream()
                            .filter(key -> !visited.contains(key))
                            .forEach(key -> columns.get(key).values().add(null));
                    records.increment();
                }
                if (!(records.get() == 1 && columns.size() == 1)) {
                    columns.values().forEach(mutableDataFrame::addColumn);
                }
            }
            result.add(mutableDataFrame);
        }
        return result;
    }

    @Override
    public List<TimeSeriesEntry> getPriceL2(@Nonnull PricesL2Query query, TimeInterval timeInterval, int maxDataPoints) {
        ChartingResult result = provider.getLines(Collections.singletonList(new BookSymbolQueryImpl(query.getStream(),
                query.getSymbol(),
                ChartType.PRICES_L2,
                timeInterval,
                maxDataPoints,
                query.getLevels()))
        );
        return buildGrafanaChart(result);
    }

    @Override
    public List<TimeSeriesEntry> getBars(@Nonnull BarsQuery query, TimeInterval timeInterval, int maxDataPoints) {
        ChartingResult result = provider.getLines(Collections.singletonList(new BookSymbolQueryImpl(query.getStream(),
                query.getSymbol(),
                ChartType.BARS,
                timeInterval,
                maxDataPoints,
                10))
        );
        return buildGrafanaBars(result);
    }

    @Override
    public Collection<String> listStreams(String template, int limit) {
        if (StringUtil.isEmpty(template)) {
            return Arrays.stream(timebase.listStreams())
                    .map(DXTickStream::getKey)
                    .filter(this::isKeyAccepted)
                    .sorted()
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            return Arrays.stream(timebase.listStreams())
                    .map(DXTickStream::getKey)
                    .filter(this::isKeyAccepted)
                    .filter(key -> key.toLowerCase().contains(template.toLowerCase()))
                    .sorted()
                    .limit(limit)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Collection<String> listSymbols(String streamKey, String template, int limit) throws NoSuchStreamException {
        DXTickStream stream = timebase.getStream(streamKey);
        if (stream == null) {
            throw new NoSuchStreamException(streamKey);
        }
        if (StringUtil.isEmpty(template)) {
            return Arrays.stream(stream.listEntities())
                    .map(entity -> entity.getSymbol().toString())
                    .limit(limit)
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            return Arrays.stream(stream.listEntities())
                    .map(entity -> entity.getSymbol().toString())
                    .filter(symbol -> symbol.toLowerCase().contains(template.toLowerCase()))
                    .limit(limit)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    @Override
    public StreamSchema schema(String streamKey) throws NoSuchStreamException {
        DXTickStream stream = timebase.getStream(streamKey);
        if (stream == null) {
            throw new NoSuchStreamException(streamKey);
        }
        StreamSchema schema = new StreamSchema();
        schema.setTypes(listFields(stream));
        schema.setFunctions(functionsService.listFunctions(streamKey));
        return schema;
    }

    @Override
    public Collection<String> aggregations() {
        return Arrays.stream(AggregationType.values()).map(Enum::name).collect(Collectors.toList());
    }

    @Override
    public StreamInfo streamInfo(String streamKey) {
        DXTickStream stream = timebase.getStream(streamKey);
        if (stream == null) {
            return null;
        }
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.setSymbols(listSymbols(stream));
        Map<String, List<String>> numericFields = listNumericFields(stream);
        streamInfo.setNumericFields(numericFields);
        streamInfo.setTypes(numericFields.keySet().stream().sorted().collect(Collectors.toList()));
        return streamInfo;
    }

    private void checkSymbols(@Nonnull DXTickStream stream, List<String> symbols) throws NoSuchSymbolsException {
        if (symbols == null || symbols.isEmpty()) {
            return;
        }
        Set<String> allSymbols = Arrays.stream(stream.listEntities())
                .map(IdentityKey::getSymbol)
                .map(CharSequence::toString)
                .collect(Collectors.toSet());
        List<String> filtered = symbols.stream()
                .filter(s -> !allSymbols.contains(s))
                .collect(Collectors.toList());
        if (!filtered.isEmpty())
            throw new NoSuchSymbolsException(stream.getKey(), filtered);
    }

    private SelectBuilder2 constructQuery(String streamKey, TimeRange range, SelectQuery query)
            throws NoSuchStreamException, NoSuchSymbolsException, SelectBuilder2.NoSuchFieldException, WrongTypeException, NoSuchTypeException {
        DXTickDB db = timebase.getConnection();
        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
            throw new NoSuchStreamException(streamKey);
        }
        checkSymbols(stream, query.getSymbols());
        SelectBuilder2 selectBuilder = SelectBuilder2.builder(db, stream)
                .setSymbols(query.getSymbols())
                .timeBetween(range.getFrom(), range.getTo());
        if (query.getFields() != null) {
            for (Map.Entry<String, List<TBField>> entry : query.getFields().entrySet()) {
                selectBuilder.type(entry.getKey())
                        .selectFields(entry.getValue().stream().map(TBField::getName).collect(Collectors.toList()));
            }
        }
        if (query.getFilters() != null) {
            for (Map.Entry<String, List<FieldFilter>> entry : query.getFilters().entrySet()) {
                Type type = selectBuilder.type(entry.getKey());
                for (FieldFilter fieldFilter : entry.getValue()) {
                    Type.Field field = type.field(fieldFilter.getFieldName());
                    switch (fieldFilter.getFilterType()) {
                        case EQUAL:
                            field.equalTo(fieldFilter.getValues().get(0));
                            break;
                        case NOTEQUAL:
                            field.notEqualTo(fieldFilter.getValues().get(0));
                            break;
                        case GREATER:
                            field.greaterThan(fieldFilter.getValues().get(0));
                            break;
                        case NOTGREATER:
                            field.notGreaterThan(fieldFilter.getValues().get(0));
                            break;
                        case LESS:
                            field.lessThan(fieldFilter.getValues().get(0));
                            break;
                        case NOTLESS:
                            field.notLessThan(fieldFilter.getValues().get(0));
                            break;
                        case IN:
                            field.equalTo(fieldFilter.getValues());
                            break;
                        case NULL:
                            field.isNull();
                            break;
                        case NOTNULL:
                            field.notNull();
                            break;
                        case STARTS_WITH:
                            field.startsWith(fieldFilter.getValues().get(0));
                            break;
                        case ENDS_WITH:
                            field.endsWith(fieldFilter.getValues().get(0));
                            break;
                        case CONTAINS:
                            field.contains(fieldFilter.getValues().get(0));
                            break;
                        case NOT_CONTAINS:
                            field.notContains(fieldFilter.getValues().get(0));
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
            }
        }
        return selectBuilder;
    }

    private List<TimeSeriesEntry> select(String streamKey, TimeRange range, long step, SelectQuery query)
            throws NoSuchStreamException, NoSuchSymbolsException, SelectBuilder2.NoSuchFieldException, WrongTypeException, NoSuchTypeException {
        SelectBuilder2 selectBuilder = constructQuery(streamKey, range, query);
        try (MessageSource<InstrumentMessage> messageSource = selectBuilder.executeRaw()) {
            Map<String, TimeSeriesEntry> entries = new HashMap<>();
            Map<String, List<Aggregation>> aggregations = new HashMap<>();
            query.getFields().values().stream()
                    .flatMap(Collection::stream)
                    .forEach(tbField -> aggregations.put(
                            tbField.getName(),
                            tbField.getAggregations().stream()
                                    .map(AggregationInfo::getName)
                                    .map(Aggregations::fromString)
                                    .collect(Collectors.toList())
                    ));
            Aggregator aggregator = new MultiAggregator(range.getFrom().toEpochMilli(), range.getTo().toEpochMilli(), step, aggregations);
            IntervalEntry entry = new IntervalEntry();
            HashSet<String> visited = new HashSet<>();
            while (aggregator.nextInterval(messageSource, entry)) {
                visited.clear();
                entry.getValues().keyIterator().forEachRemaining(key -> {
                    entries.computeIfAbsent(key, k -> new TimeSeriesEntry(key)).datapoints
                            .add(new Number[]{entry.getValues().get(key, Double.NaN), entry.getTimestamp()});
                    visited.add(key);
                });
                entries.keySet().stream()
                        .filter(key -> !visited.contains(key))
                        .forEach(key -> entries.get(key).datapoints.add(new Number[]{null, entry.getTimestamp()}));
            }
            entries.forEach((key, value) -> {
                LOG.info().append(key).append(": ").append(value.datapoints.size()).commit();
            });

            return new ArrayList<>(entries.values());
        }
    }

    private List<String> listSymbols(@Nonnull DXTickStream stream) {
        return Arrays.stream(stream.listEntities())
                .map(entity -> entity.getSymbol().toString())
                .collect(Collectors.toList());
    }

    private List<String> listTypes(@Nonnull DXTickStream stream) {
        return Arrays.stream(stream.getTypes())
                .map(RecordClassDescriptor::getName)
                .collect(Collectors.toList());
    }

    private static Map<String, List<String>> listNumericFields(@Nonnull DXTickStream stream) {
        Map<String, List<String>> result = new HashMap<>();
        for (RecordClassDescriptor type : stream.getTypes()) {
            listNumericFields(type, result);
        }
        return result;
    }

    private static Collection<TypeInfo> listFields(@Nonnull DXTickStream stream) {
        Set<String> looked = new HashSet<>();
        List<TypeInfo> result = new ArrayList<>();
        result.add(INSTRUMENT_MSG);
        CharSequenceToIntHashMap typeCounts = countTypes(stream);
        for (RecordClassDescriptor type : stream.getTypes()) {
            listFields(type, looked, result, typeCounts);
        }
        return result;
    }

    private static TypeInfo instrumentMessage() {
        TypeInfo typeInfo = new TypeInfo();
        typeInfo.setType("InstrumentMessage");
        TypeInfo.FieldInfo fieldInfo = new TypeInfo.FieldInfo();
        fieldInfo.setName("symbol");
        TypeInfo.FieldType fieldType = new TypeInfo.FieldType();
        fieldType.setDataType(ValueType.VARCHAR);
        fieldInfo.setFieldType(fieldType);
        typeInfo.setFields(Collections.singletonList(fieldInfo));
        return typeInfo;
    }

    private static CharSequenceToIntHashMap countTypes(DXTickStream stream) {
        Set<String> looked = new HashSet<>();
        CharSequenceToIntHashMap map = new CharSequenceToIntHashMap(0);
        for (RecordClassDescriptor type : stream.getTypes()) {
            countTypes(type, looked, map);
        }
        return map;
    }

    private static void countTypes(RecordClassDescriptor type, Set<String> looked, CharSequenceToIntHashMap map) {
        if (type == null)
            return;
        String shortType = shortType(type.getName());
        if (looked.add(type.getName())) {
            map.set(shortType, map.get(shortType) + 1);
        }
        countTypes(type.getParent(), looked, map);
    }

    private static String shortType(String fullType) {
        return fullType.substring(fullType.lastIndexOf(".") + 1);
    }

    private static void listFields(RecordClassDescriptor rcd, Set<String> looked, List<TypeInfo> types, CharSequenceToIntHashMap typeCounts) {
        if (rcd == null)
            return;
        if (!looked.contains(rcd.getName())) {
            looked.add(rcd.getName());
            TypeInfo typeInfo = new TypeInfo();
            String shortType = shortType(rcd.getName());
            typeInfo.setType(typeCounts.get(shortType) <= 1 ? shortType : rcd.getName());
            typeInfo.setFields(Arrays.stream(rcd.getFields())
                    .filter(NonStaticDataField.class::isInstance)
                    .map(NonStaticDataField.class::cast)
                    .filter(GrafanaServiceImpl::isValid)
                    .map(field -> {
                        TypeInfo.FieldInfo fieldInfo = new TypeInfo.FieldInfo();
                        fieldInfo.setName(field.getName());
                        fieldInfo.setFieldType(TypeInfo.fieldType(field.getType()));
                        return fieldInfo;
                    })
                    .collect(Collectors.toList()));
            if (!typeInfo.getFields().isEmpty()) {
                types.add(typeInfo);
            }
            listFields(rcd.getParent(), looked, types, typeCounts);
        }
    }

    private static void listNumericFields(RecordClassDescriptor rcd, Map<String, List<String>> map) {
        if (rcd == null)
            return;
        map.put(rcd.getName(), Arrays.stream(rcd.getFields())
                .filter(NonStaticDataField.class::isInstance)
                .map(NonStaticDataField.class::cast)
                .filter(GrafanaServiceImpl::isNumeric)
                .map(NonStaticDataField::getName)
                .sorted()
                .collect(Collectors.toList()));
        listNumericFields(rcd.getParent(), map);
    }

    private static boolean isNumeric(DataField dataField) {
        return dataField.getType() instanceof IntegerDataType
                || dataField.getType() instanceof FloatDataType
                || dataField.getType() instanceof BooleanDataType;
    }

    private static boolean isValid(DataField dataField) {
        return dataField.getType() instanceof IntegerDataType
                || dataField.getType() instanceof FloatDataType
                || dataField.getType() instanceof BooleanDataType
                || dataField.getType() instanceof VarcharDataType
                || dataField.getType() instanceof EnumDataType
                || dataField.getType() instanceof DateTimeDataType
                || dataField.getType() instanceof TimeOfDayDataType;
    }

    private static boolean isSelectable(DataField dataField) {
        return isNumeric(dataField);
    }

    private static boolean isFilterable(DataField dataField) {
        return isValid(dataField);
    }

    public Set<String> getStreams() {
        return streams;
    }

    public void setStreams(Set<String> streams) {
        this.streams = streams;
    }

    public String getInclude() {
        return include;
    }

    public void setInclude(String include) {
        this.include = include;
        if (include != null && include.length() > 0)
            this.iPattern = Pattern.compile(include);
    }

    public String getExclude() {
        return exclude;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
        if (exclude != null && exclude.length() > 0)
            this.ePattern = Pattern.compile(exclude);
    }

    private boolean isKeyAccepted(String key) {
        if (streams.isEmpty()) {
            boolean matches = true;
            if (iPattern != null)
                matches = iPattern.matcher(key).find();
            if (ePattern != null)
                matches &= !ePattern.matcher(key).find();
            return matches;
        }
        return streams.contains(key);
    }

    private List<TimeSeriesEntry> buildGrafanaChart(ChartingResult chartingResult) {

        LinesQueryResult result = chartingResult.results().get(0);

        List<TimeSeriesEntry> list = new ObjectArrayList<>();

        result.getLines().forEach(lineResult -> {
            TimeSeriesEntry entry = new TimeSeriesEntry(lineResult.getName());
            lineResult.getPoints().subscribe(message -> {
                if (message instanceof LinePointDef) {
                    LinePointDef linePointDef = (LinePointDef) message;
                    entry.datapoints.add(new Number[]{
                            Double.parseDouble(linePointDef.getValue()),
                            linePointDef.getTime()
                    });
                } else if (message instanceof TagElementDef) {
                    TagElementDef tag = (TagElementDef) message;
                    entry.datapoints.add(new Number[]{
                            Double.parseDouble(tag.getValue()),
                            tag.getTime()
                    });
                }
            });
            list.add(entry);
        });
        chartingResult.run();

        return list;
    }

    private List<TimeSeriesEntry> buildGrafanaBars(ChartingResult chartingResult) {
        LinesQueryResult result = chartingResult.results().get(0);

        List<TimeSeriesEntry> list = new ObjectArrayList<>();
        TimeSeriesEntry open = new TimeSeriesEntry("open");
        list.add(open);
        TimeSeriesEntry close = new TimeSeriesEntry("close");
        list.add(close);
        TimeSeriesEntry high = new TimeSeriesEntry("high");
        list.add(high);
        TimeSeriesEntry low = new TimeSeriesEntry("low");
        list.add(low);

        result.getLines().forEach(lineResult -> {
            lineResult.getPoints().subscribe(message -> {
                if (message instanceof BarElementDef) {
                    BarElementDef barElementDef = (BarElementDef) message;
                    open.datapoints.add(new Number[]{
                            Double.parseDouble(barElementDef.getOpen()),
                            barElementDef.getTime()
                    });
                    close.datapoints.add(new Number[]{
                            Double.parseDouble(barElementDef.getClose()),
                            barElementDef.getTime()
                    });
                    high.datapoints.add(new Number[]{
                            Double.parseDouble(barElementDef.getHigh()),
                            barElementDef.getTime()
                    });
                    low.datapoints.add(new Number[]{
                            Double.parseDouble(barElementDef.getLow()),
                            barElementDef.getTime()
                    });
                }
            });
        });
        chartingResult.run();

        return list;
    }

    private List<TimeSeriesEntry> buildSimple(
            String streamKey,
            List<String> symbols,
            TimeInterval timeInterval,
            int maxPoints,
            Map<String, List<String>> typesToFields,
            AggregationType aggregationType
    ) {
        DXTickDB db = timebase.getConnection();
        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
            LOG.error().append("There's no stream ").append(streamKey).commit();
            return Collections.emptyList();
        }
        return buildSimple(db, stream, symbols, timeInterval, maxPoints, typesToFields, aggregationType);
    }

    private List<TimeSeriesEntry> buildSimple(
            DXTickDB db,
            DXTickStream stream,
            List<String> symbols,
            TimeInterval timeInterval,
            int maxPoints,
            Map<String, List<String>> typesToFields,
            AggregationType aggregationType
    ) {
        SelectBuilder2 selectBuilder = SelectBuilder2.builder(db, stream)
                .setSymbols(symbols)
                .timeBetween(timeInterval.getStartTime(), timeInterval.getEndTime());
        for (Map.Entry<String, List<String>> entry : typesToFields.entrySet()) {
            try {
                selectBuilder.type(entry.getKey())
                        .selectFields(entry.getValue());
            } catch (SelectBuilder2.NoSuchFieldException | NoSuchTypeException e) {
                LOG.error().append(e).commit();
                return Collections.emptyList();
            }
        }
        try (MessageSource<InstrumentMessage> messageSource = selectBuilder.executeRaw()) {
            return fromMessageSource(messageSource, maxPoints, timeInterval, aggregationType);
        }
    }

    private List<TimeSeriesEntry> fromMessageSource(
            MessageSource<InstrumentMessage> messageSource,
            int maxPoints,
            TimeInterval timeInterval,
            @Nonnull AggregationType aggregationType
    ) {
        Map<String, TimeSeriesEntry> entries = new HashMap<>();
        Aggregator aggregator = new AggregatorImpl(timeInterval.getStartTimeMilli(), timeInterval.getEndTimeMilli(), maxPoints, Aggregations.fromType(aggregationType));
        IntervalEntry entry = new IntervalEntry();
        Set<String> visited = new HashSet<>();
        while (aggregator.nextInterval(messageSource, entry)) {
            visited.clear();
            entry.getValues().keyIterator().forEachRemaining(key -> {
                entries.computeIfAbsent(key, k -> new TimeSeriesEntry(key)).datapoints
                        .add(new Number[]{entry.getValues().get(key, Double.NaN), entry.getTimestamp()});
                visited.add(key);
            });
            entries.keySet().stream()
                    .filter(key -> !visited.contains(key))
                    .forEach(key -> entries.get(key).datapoints.add(new Number[]{null, entry.getTimestamp()}));
        }
        entries.forEach((key, value) -> {
            LOG.info().append(key).append(": ").append(value.datapoints.size()).commit();
        });

        return new ArrayList<>(entries.values());
    }

    private static long calculateStep(SelectQuery query, DataQueryRequest<SelectQuery> request) {
        long baseStep = Aggregator.calculateStep(request.getRange(), request.getMaxDataPoints());
        long step;
        if (query.getInterval() == null || query.getInterval().getIntervalType() == SelectQuery.IntervalType.MAX_DATA_POINTS) {
            step = baseStep;
        } else if (query.getInterval().getIntervalType() == SelectQuery.IntervalType.MILLISECONDS) {
            step = query.getInterval().getValue() == null ? baseStep : Math.max(query.getInterval().getValue(), baseStep);
        } else {
            step = request.getRange().getTo().toEpochMilli() - request.getRange().getFrom().toEpochMilli();
        }
        return step;
    }
}
