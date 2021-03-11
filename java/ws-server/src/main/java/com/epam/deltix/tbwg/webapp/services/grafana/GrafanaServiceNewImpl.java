package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.anvil.util.StringUtil;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.tbwg.webapp.model.grafana.DynamicList;
import com.epam.deltix.tbwg.webapp.model.grafana.filters.FieldFilter;
import com.epam.deltix.tbwg.webapp.model.grafana.queries.SelectQuery;
import com.epam.deltix.tbwg.webapp.model.grafana.time.TimeRange;
import com.epam.deltix.tbwg.webapp.services.TimebaseServiceImpl;
import com.epam.deltix.tbwg.webapp.services.grafana.base.FunctionsService;
import com.epam.deltix.tbwg.webapp.services.grafana.base.GrafanaServiceNew;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.NoSuchStreamException;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.NoSuchSymbolsException;
import com.epam.deltix.tbwg.webapp.services.grafana.exc.ValidationException;
import com.epam.deltix.tbwg.webapp.services.grafana.qql.SelectBuilder2;
import com.epam.deltix.tbwg.webapp.utils.GrafanaStreamCreator;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.computations.base.exc.RecordValidationException;
import com.epam.deltix.computations.data.MutableGenericRecordImpl;
import com.epam.deltix.computations.data.RawMessageDecoder;
import com.epam.deltix.computations.data.base.GenericRecord;
import com.epam.deltix.computations.data.base.GenericValueInfo;
import com.epam.deltix.computations.data.base.MutableGenericRecord;
import com.epam.deltix.grafana.GroupByAggregation;
import com.epam.deltix.grafana.base.Aggregation;
import com.epam.deltix.grafana.data.MapBasedDataFrame;
import com.epam.deltix.grafana.data.MutableDataFrameImpl;
import com.epam.deltix.grafana.model.DataFrame;
import com.epam.deltix.grafana.model.fields.ColumnImpl;
import com.epam.deltix.grafana.model.fields.Field;
import com.epam.deltix.grafana.model.fields.FieldType;
import com.epam.deltix.grafana.model.fields.MutableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@ConfigurationProperties(prefix = "grafana")
public class GrafanaServiceNewImpl implements GrafanaServiceNew {

    private static final Log LOG = LogFactory.getLog(GrafanaServiceNewImpl.class);

    private final TimebaseServiceImpl timebase;
    private final FunctionsService functionsService;

    private Set<String> streams = new HashSet<>();
    private String include;
    private String exclude;
    private Pattern iPattern;
    private Pattern ePattern;

    @Autowired
    public GrafanaServiceNewImpl(TimebaseServiceImpl timebase, FunctionsService functionsService) {
        this.timebase = timebase;
        this.functionsService = functionsService;
    }

    @PostConstruct
    public void postConstruct() {
        if (Boolean.getBoolean("grafana.testStream")) {
            GrafanaStreamCreator creator = new GrafanaStreamCreator(new String[]{});
            creator.createAndLoad(timebase.getConnection(), "GrafanaTestStream");
        }
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
            iPattern = Pattern.compile(include);
        else
            iPattern = null;
    }

    public String getExclude() {
        return exclude;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
        if (exclude != null && exclude.length() > 0)
            ePattern = Pattern.compile(exclude);
        else
            ePattern = null;
    }

    @Override
    public DataFrame dataFrame(SelectQuery query, TimeRange range, int maxDataPoints, Long intervalMs) throws ValidationException,
            RecordValidationException {
        SelectBuilder2 selectBuilder = constructQuery(query, range);
        long step = calculateStep(query, range, maxDataPoints, intervalMs);
        if (query.getFunctions() == null || query.getFunctions().isEmpty()) {
            return new MutableDataFrameImpl(query.getRefId());
        }
        boolean groupBy = query.getGroupBy() != null && !query.getGroupBy().isEmpty();
        Aggregation aggregation = null;
        TreeMap<Long, List<Map<String, Object>>> resultMap = new TreeMap<>();
        RawMessageDecoder decoder = new RawMessageDecoder();
        StringBuilder keyBuilder = new StringBuilder();
        Map<String, Field> fields = new HashMap<>();
        GroupByViewOption groupByViewOption = StringUtil.isNotEmpty(query.getGroupByView()) ?
                GroupByViewOption.valueOf(query.getGroupByView()) : GroupByViewOption.COLUMN;
        try (MessageSource<InstrumentMessage> messageSource = selectBuilder.executeRaw()) {
            MutableGenericRecord record = new MutableGenericRecordImpl();
            boolean first = true;
            while (messageSource.next()) {
                if (first) {
                    first = false;
                    aggregation = functionsService.aggregation(query, messageSource.getMessage().getTimeStampMs(),
                            range.getTo().toEpochMilli(), step, query.getGroupBy(), query.getSymbols());
                }
                decoder.decode((RawMessage) messageSource.getMessage(), record);
                if (groupBy) {
                    keyBuilder.setLength(0);
                    keyBuilder.append("[");
                    for (SelectQuery.TimebaseField timebaseField : query.getGroupBy()) {
                        GenericValueInfo value = record.getValue(timebaseField.getName());
                        keyBuilder.append(value == null ? null : value.charSequenceValue()).append("*");
                        if (groupByViewOption == GroupByViewOption.ROW) {
                            fields.put(timebaseField.toString(), new MutableField(timebaseField.toString(), FieldType.STRING));
                        }
                    }
                    keyBuilder.setLength(keyBuilder.length() - 1);
                    keyBuilder.append("]");
                    record.setRecordKey(keyBuilder.toString());
                }
                if (aggregation.add(record)) {
                    GenericRecord out = aggregation.record();
                    Map<String, Object> map = new HashMap<>(aggregation.fields().size());
                    if (!groupBy) {
                        for (Field field : aggregation.fields()) {
                            fields.putIfAbsent(field.name(), field);
                            if (out.getValue(field.name()) != null) {
                                map.put(field.name(), out.getValue(field.name()).value());
                            }
                        }
                        resultMap.computeIfAbsent(out.timestamp(), key -> {
                            List<Map<String, Object>> list = new ObjectArrayList<>();
                            list.add(new HashMap<>());
                            return list;
                        }).get(0).putAll(map);
                    } else {
                        switch (groupByViewOption) {
                            case COLUMN:
                                for (Field field : aggregation.fields()) {
                                    fields.computeIfAbsent(field.name() + record.recordKey(), key -> new ColumnImpl(key, field.type()));
                                    if (out.getValue(field.name()) != null) {
                                        map.put(field.name() + record.recordKey(), out.getValue(field.name()).value());
                                    }
                                }
                                resultMap.computeIfAbsent(out.timestamp(), key -> {
                                    List<Map<String, Object>> list = new ObjectArrayList<>();
                                    list.add(new HashMap<>());
                                    return list;
                                }).get(0).putAll(map);
                                break;
                            case ROW: {
                                for (Field field : aggregation.fields()) {
                                    fields.computeIfAbsent(field.name(), key -> new ColumnImpl(key, field.type()));
                                    if (out.getValue(field.name()) != null) {
                                        map.put(field.name(), out.getValue(field.name()).value());
                                    }
                                }
                                for (SelectQuery.TimebaseField timebaseField : query.getGroupBy()) {
                                    map.put(timebaseField.toString(), record.getValue(timebaseField.getName()).value());
                                }
                                List<Map<String, Object>> list = resultMap.computeIfAbsent(out.timestamp(),
                                        key -> new ObjectArrayList<>());
                                list.add(map);
                                break;
                            }
                            default:
                                throw new UnsupportedOperationException();
                        }
                    }
                }
            }
            if (first) {
                return new MutableDataFrameImpl(query.getRefId());
            }
            if (aggregation instanceof GroupByAggregation) {
                GroupByAggregation groupByAggregation = (GroupByAggregation) aggregation;
                for (Map.Entry<String, GenericRecord> entry : groupByAggregation.calculateLastRecords().entrySet()) {
                    GenericRecord out = entry.getValue();
                    if (out != null) {
                        Map<String, Object> map = new HashMap<>(aggregation.fields().size());
                        switch (groupByViewOption) {
                            case COLUMN:
                                for (Field field : aggregation.fields()) {
                                    fields.computeIfAbsent(field.name() + entry.getKey(), key -> new ColumnImpl(key, field.type()));
                                    if (out.getValue(field.name()) != null) {
                                        map.put(field.name() + entry.getKey(), out.getValue(field.name()).value());
                                    }
                                }
                                resultMap.computeIfAbsent(out.timestamp(), key -> {
                                    List<Map<String, Object>> list = new ObjectArrayList<>();
                                    list.add(new HashMap<>());
                                    return list;
                                }).get(0).putAll(map);
                                break;
                            case ROW: {
                                String[] values = entry.getKey().substring(1, entry.getKey().length() - 1).split("\\*");
                                for (Field field : aggregation.fields()) {
                                    fields.computeIfAbsent(field.name(), key -> new ColumnImpl(key, field.type()));
                                    if (out.getValue(field.name()) != null) {
                                        map.put(field.name(), out.getValue(field.name()).value());
                                    }
                                }
                                int j = 0;
                                for (SelectQuery.TimebaseField timebaseField : query.getGroupBy()) {
                                    map.put(timebaseField.toString(), values[j++]);
                                }
                                List<Map<String, Object>> list = resultMap.computeIfAbsent(out.timestamp(),
                                        key -> new ObjectArrayList<>());
                                list.add(map);
                                break;
                            }
                            default:
                                throw new UnsupportedOperationException();
                        }
                    }
                }
            } else {
                GenericRecord out = aggregation.calculateLast();
                if (out != null) {
                    Map<String, Object> map = new HashMap<>(aggregation.fields().size());
                    for (Field field : aggregation.fields()) {
                        fields.putIfAbsent(field.name(), field);
                        map.put(field.name(), out.getValue(field.name()).value());
                    }
                    resultMap.computeIfAbsent(out.timestamp(), key -> {
                        List<Map<String, Object>> list = new ObjectArrayList<>();
                        list.add(new HashMap<>());
                        return list;
                    }).get(0).putAll(map);
                }
            }
        }
        MapBasedDataFrame dataFrame = new MapBasedDataFrame(query.getRefId(),
                fields.values().stream().sorted(Comparator.comparing(Field::name)).collect(Collectors.toList()));
        dataFrame.append(Collections.singletonList(resultMap));
        return dataFrame;
    }

    @Override
    public List<String> groupByViewOptions() {
        return Arrays.stream(GroupByViewOption.values()).map(Enum::name).collect(Collectors.toList());
    }

    @Override
    public DynamicList listStreams(String template, int offset, int limit) {
        DynamicList result = new DynamicList();
        List<String> list = new ObjectArrayList<>();
        result.setList(list);
        result.setHasMore(false);
        List<String> streams;
        if (StringUtil.isEmpty(template)) {
            streams = Arrays.stream(timebase.listStreams())
                    .map(DXTickStream::getKey)
                    .filter(this::isKeyAccepted)
                    .sorted()
                    .skip(offset)
                    .collect(Collectors.toList());
        } else {
            streams = Arrays.stream(timebase.listStreams())
                    .map(DXTickStream::getKey)
                    .filter(this::isKeyAccepted)
                    .filter(key -> key.toLowerCase().contains(template.toLowerCase()))
                    .sorted()
                    .skip(offset)
                    .collect(Collectors.toList());
        }
        for (String stream : streams) {
            if (list.size() == limit) {
                result.setHasMore(true);
                break;
            }
            list.add(stream);
        }
        return result;
    }

    @Override
    public DynamicList listSymbols(String streamKey, String template, int offset, int limit) throws NoSuchStreamException {
        DXTickStream stream = timebase.getStream(streamKey);
        if (stream == null) {
            throw new NoSuchStreamException(streamKey);
        }
        DynamicList result = new DynamicList();
        List<String> list = new ObjectArrayList<>();
        result.setList(list);
        result.setHasMore(false);
        List<String> symbols;
        if (StringUtil.isEmpty(template)) {
            symbols = Arrays.stream(stream.listEntities())
                    .map(entity -> entity.getSymbol().toString())
                    .sorted()
                    .skip(offset)
                    .collect(Collectors.toList());
        } else {
            symbols = Arrays.stream(stream.listEntities())
                    .map(entity -> entity.getSymbol().toString())
                    .filter(symbol -> symbol.toLowerCase().contains(template.toLowerCase()))
                    .sorted()
                    .skip(offset)
                    .collect(Collectors.toList());
        }
        for (String symbol : symbols) {
            if (list.size() == limit) {
                result.setHasMore(true);
                break;
            }
            list.add(symbol);
        }
        return result;
    }

    private SelectBuilder2 constructQuery(SelectQuery query, TimeRange range) throws NoSuchStreamException,
            NoSuchSymbolsException, SelectBuilder2.NoSuchFieldException, SelectBuilder2.WrongTypeException,
            SelectBuilder2.NoSuchTypeException {
        DXTickDB db = timebase.getConnection();
        DXTickStream stream = db.getStream(query.getStream());
        if (stream == null) {
            throw new NoSuchStreamException(query.getStream());
        }
        checkSymbols(stream, query.getSymbols());
        SelectBuilder2 selectBuilder = SelectBuilder2.builder(db, stream)
                .setSymbols(query.getSymbols())
                .timeBetween(range.getFrom(), range.getTo());
        if (!query.getGroupBy().isEmpty()) {
            for (SelectQuery.TimebaseField timebaseField : query.getGroupBy()) {
                selectBuilder.type(timebaseField.getType()).field(timebaseField.getName()).select();
            }
        }
        selectFields(selectBuilder, query.getFunctions());
        appendFilters(selectBuilder, query.getFilters());
        return selectBuilder;
    }

    private void selectFields(SelectBuilder2 selectBuilder2, List<SelectQuery.FunctionDef> functions)
            throws SelectBuilder2.NoSuchFieldException, SelectBuilder2.NoSuchTypeException {
        if (functions != null) {
            for (SelectQuery.FunctionDef function : functions) {
                selectFields(selectBuilder2, function);
            }
        }
    }

    private boolean selectFields(SelectBuilder2 selectBuilder2, SelectQuery.FunctionDef function)
            throws SelectBuilder2.NoSuchTypeException, SelectBuilder2.NoSuchFieldException {
        if (function.getFieldArgs() == null || function.getFieldArgs().isEmpty()) {
            selectBuilder2.selectAll();
            return true;
        }
        for (SelectQuery.FieldArg fieldArg : function.getFieldArgs()) {
            if (fieldArg.getField() != null) {
                selectBuilder2.type(fieldArg.getField().getType()).field(fieldArg.getField().getName()).select();
            } else if (fieldArg.getFunction() != null) {
                if (selectFields(selectBuilder2, fieldArg.getFunction())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void appendFilters(SelectBuilder2 selectBuilder, Map<String, List<FieldFilter>> filters) throws SelectBuilder2.NoSuchFieldException, SelectBuilder2.NoSuchTypeException, SelectBuilder2.WrongTypeException {
        if (filters != null) {
            for (Map.Entry<String, List<FieldFilter>> entry : filters.entrySet()) {
                SelectBuilder2.Type type = selectBuilder.type(entry.getKey());
                for (FieldFilter fieldFilter : entry.getValue()) {
                    SelectBuilder2.Type.Field field = type.field(fieldFilter.getFieldName());
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
                        case NOT_IN:
                            field.notEqualTo(fieldFilter.getValues());
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

    private static long calculateStep(SelectQuery query, TimeRange timeRange, int maxDataPoints, Long intervalMs) {
        long baseStep = intervalMs == null ? Aggregator.calculateStep(timeRange, maxDataPoints): intervalMs;
        long step;
        if (query.getInterval() == null || query.getInterval().getIntervalType() == SelectQuery.IntervalType.MAX_DATA_POINTS) {
            step = baseStep;
        } else if (query.getInterval().getIntervalType() == SelectQuery.IntervalType.MILLISECONDS) {
            step = query.getInterval().getValue() == null ? baseStep : Math.max(query.getInterval().getValue(), baseStep);
        } else {
            step = timeRange.getTo().toEpochMilli() - timeRange.getFrom().toEpochMilli();
        }
        return step;
    }

    enum GroupByViewOption {
        COLUMN, ROW
    }

}
