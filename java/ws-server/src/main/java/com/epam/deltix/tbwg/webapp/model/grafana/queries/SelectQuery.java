package com.epam.deltix.tbwg.webapp.model.grafana.queries;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.epam.deltix.tbwg.webapp.model.grafana.TBField;
import com.epam.deltix.tbwg.webapp.model.grafana.filters.FieldFilter;

import java.util.List;
import java.util.Map;

/**
 * Select query, that allows selecting separate fields, filtering by plain fields (not arrays, objects and binaries)
 * and aggregations by time interval.
 */
public class SelectQuery extends TBQuery {

    /**
     * Symbols list. If empty - query is performed over all symbols.
     */
    @JsonProperty
    protected List<String> symbols;

    /**
     * Types list. If empty - query is performed over all types.
     */
    @JsonProperty
    protected List<String> types;

    /**
     * Types to lists of fields map.
     */
    @JsonProperty
    protected Map<String, List<TBField>> fields;

    @JsonProperty
    protected List<FunctionDef> functions;

    /**
     * Time interval for aggregation. If null - calculated dynamically.
     */
    @JsonProperty
    protected AggregationInterval interval;

    /**
     * Types to list of field filters.
     */
    @JsonProperty
    protected Map<String, List<FieldFilter>> filters;

    /**
     * List of fields, that group by is performed over.
     */
    @JsonProperty
    private List<TimebaseField> groupBy;

    /**
     * GroupBy view option.
     */
    @JsonProperty
    private String groupByView;

    @JsonProperty
    private View view;

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public Map<String, List<TBField>> getFields() {
        return fields;
    }

    public void setFields(Map<String, List<TBField>> fields) {
        this.fields = fields;
    }

    public List<FunctionDef> getFunctions() {
        return functions;
    }

    public void setFunctions(List<FunctionDef> functions) {
        this.functions = functions;
    }

    public AggregationInterval getInterval() {
        return interval;
    }

    public void setInterval(AggregationInterval interval) {
        this.interval = interval;
    }

    public Map<String, List<FieldFilter>> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, List<FieldFilter>> filters) {
        this.filters = filters;
    }

    public List<TimebaseField> getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(List<TimebaseField> groupBy) {
        this.groupBy = groupBy;
    }

    public String getGroupByView() {
        return groupByView;
    }

    public void setGroupByView(String groupByView) {
        this.groupByView = groupByView;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public static class AggregationInterval {

        @JsonProperty
        private IntervalType intervalType;

        @JsonProperty
        private Long value;

        public IntervalType getIntervalType() {
            return intervalType;
        }

        public void setIntervalType(IntervalType intervalType) {
            this.intervalType = intervalType;
        }

        public Long getValue() {
            return value;
        }

        public void setValue(Long value) {
            this.value = value;
        }
    }

    public enum IntervalType {
        MAX_DATA_POINTS, FULL_INTERVAL, MILLISECONDS
    }

    public static class FunctionDef {

        @JsonProperty
        private String id;

        @JsonProperty
        private String name;

        @JsonProperty
        private List<FieldArg> fieldArgs;

        @JsonProperty
        private Map<String, String> constantArgs;

        @JsonProperty
        private String resultField;

        @JsonProperty
        private Map<String, String> resultFields;

        @JsonProperty
        private List<TimebaseField> groupBy;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<FieldArg> getFieldArgs() {
            return fieldArgs;
        }

        public void setFieldArgs(List<FieldArg> fieldArgs) {
            this.fieldArgs = fieldArgs;
        }

        public Map<String, String> getConstantArgs() {
            return constantArgs;
        }

        public void setConstantArgs(Map<String, String> constantArgs) {
            this.constantArgs = constantArgs;
        }

        public String getResultField() {
            return resultField;
        }

        public void setResultField(String resultField) {
            this.resultField = resultField;
        }

        public Map<String, String> getResultFields() {
            return resultFields;
        }

        public void setResultFields(Map<String, String> resultFields) {
            this.resultFields = resultFields;
        }

        public List<TimebaseField> getGroupBy() {
            return groupBy;
        }

        public void setGroupBy(List<TimebaseField> groupBy) {
            this.groupBy = groupBy;
        }
    }

    public static class FieldArg {

        @JsonProperty
        private FunctionDef function;

        @JsonProperty
        private TimebaseField field;

        public FunctionDef getFunction() {
            return function;
        }

        public void setFunction(FunctionDef function) {
            this.function = function;
        }

        public TimebaseField getField() {
            return field;
        }

        public void setField(TimebaseField field) {
            this.field = field;
        }
    }

    public static class TimebaseField {

        @JsonProperty
        private String type;

        @JsonProperty
        private String name;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return String.format("%s:%s", type, name);
        }
    }

    public enum View {
        DATAFRAME, TIMESERIES
    }
}
