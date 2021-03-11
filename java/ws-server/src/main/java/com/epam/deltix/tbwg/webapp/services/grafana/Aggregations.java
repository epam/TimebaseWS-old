package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.tbwg.webapp.model.grafana.AggregationType;
import com.epam.deltix.util.collections.generated.*;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotEmpty;

public final class Aggregations {

    static Aggregation fromType(AggregationType type) {
        switch (type) {
            case MAX:
                return max();
            case MIN:
                return min();
            case MEAN:
                return mean();
            case COUNT:
                return count();
            case FIRST:
                return first();
            case LAST:
                return last();
            case SUM:
                return sum();
            default:
                throw new UnsupportedOperationException();
        }
    }

    static Aggregation fromString(String type) {
        switch (AggregationType.valueOf(type)) {
            case MAX:
                return max();
            case MIN:
                return min();
            case MEAN:
                return mean();
            case COUNT:
                return count();
            case FIRST:
                return first();
            case LAST:
                return last();
            case SUM:
                return sum();
            default:
                throw new UnsupportedOperationException();
        }
    }

    static Aggregation fromString(String type, String as) {
        switch (AggregationType.valueOf(type)) {
            case MAX:
                return new CompareAggregation(MAX_COMP, "max", as);
            case MIN:
                return new CompareAggregation(MIN_COMP, "min", as);
            case MEAN:
                return new MeanAggregation(as);
            case COUNT:
                return count();
            case FIRST:
                return first();
            case LAST:
                return last();
            case SUM:
                return sum();
            default:
                throw new UnsupportedOperationException();
        }
    }

    static Aggregation max() {
        return MAX;
    }

    static Aggregation min() {
        return MIN;
    }

    static Aggregation mean() {
        return MEAN;
    }

    static Aggregation count() {
        return COUNT;
    }

    static Aggregation first() {
        return FIRST;
    }

    static Aggregation last() {
        return LAST;
    }

    static Aggregation sum() {
        return SUM;
    }

    private static final NumericComparator MAX_COMP = new NumericComparator(false);
    private static final NumericComparator MIN_COMP = new NumericComparator(true);
    private static final CompareAggregation MAX = new CompareAggregation(MAX_COMP, "max");
    private static final CompareAggregation MIN = new CompareAggregation(MIN_COMP, "min");
    private static final MeanAggregation MEAN = new MeanAggregation();
    private static final CountAggregation COUNT = new CountAggregation();
    private static final FirstAggregation FIRST = new FirstAggregation();
    private static final LastAggregation LAST = new LastAggregation();
    private static final SumAggregation SUM = new SumAggregation();

    private Aggregations() {
    }

    private static class NumericComparator {

        private final int greater;
        private final int lower;

        NumericComparator(boolean reverse) {
            if (reverse) {
                this.greater = -1;
                this.lower = 1;
            } else {
                this.greater = 1;
                this.lower = -1;
            }
        }

        public int compare(byte a, byte b) {
            if (a > b) {
                return greater;
            } else if (a < b) {
                return lower;
            } else {
                return 0;
            }
        }

        public int compare(short a, short b) {
            if (a > b) {
                return greater;
            } else if (a < b) {
                return lower;
            } else {
                return 0;
            }
        }

        public int compare(int a, int b) {
            if (a > b) {
                return greater;
            } else if (a < b) {
                return lower;
            } else {
                return 0;
            }
        }

        public int compare(long a, long b) {
            if (a > b) {
                return greater;
            } else if (a < b) {
                return lower;
            } else {
                return 0;
            }
        }

        public int compare(float a, float b) {
            if (a > b) {
                return greater;
            } else if (a < b) {
                return lower;
            } else {
                return 0;
            }
        }

        public int compare(double a, double b) {
            if (a > b) {
                return greater;
            } else if (a < b) {
                return lower;
            } else {
                return 0;
            }
        }
    }

    private static class CompareAggregation implements Aggregation {

        private final NumericComparator comparator;
        private final String name;
        private final String as;

        CompareAggregation(NumericComparator comparator, String name) {
            this(comparator, name, null);
        }

        CompareAggregation(NumericComparator comparator, String name, String as) {
            this.comparator = comparator;
            this.name = name;
            this.as = as;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ByteArrayList values) {
            byte result = values.getByte(0);
            for (int i = 1; i < values.size(); i++) {
                if (comparator.compare(values.getByte(i), result) > 0) {
                    result = values.getByte(i);
                }
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ShortArrayList values) {
            short result = values.getShort(0);
            for (int i = 1; i < values.size(); i++) {
                if (comparator.compare(values.getShort(i), result) > 0) {
                    result = values.getShort(i);
                }
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty IntegerArrayList values) {
            int result = values.getInteger(0);
            for (int i = 1; i < values.size(); i++) {
                if (comparator.compare(values.getInteger(i), result) > 0) {
                    result = values.getInteger(i);
                }
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty LongArrayList values) {
            long result = values.getLong(0);
            for (int i = 1; i < values.size(); i++) {
                if (comparator.compare(values.getLong(i), result) > 0) {
                    result = values.getLong(i);
                }
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty FloatArrayList values) {
            float result = values.getFloat(0);
            for (int i = 1; i < values.size(); i++) {
                if (comparator.compare(values.getFloat(i), result) > 0) {
                    result = values.getFloat(i);
                }
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty DoubleArrayList values) {
            double result = values.getDouble(0);
            for (int i = 1; i < values.size(); i++) {
                if (comparator.compare(values.getDouble(i), result) > 0) {
                    result = values.getDouble(i);
                }
            }
            return result;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAs() {
            return as;
        }
    }

    private static class MeanAggregation implements Aggregation {

        private final String as;

        public MeanAggregation() {
            this(null);
        }

        public MeanAggregation(String as) {
            this.as = as;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ByteArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += (values.getByte(i) - result) / (i + 1);
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ShortArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += (values.getShort(i) - result) / (i + 1);
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty IntegerArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += (values.getInteger(i) - result) / (i + 1);
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty LongArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += (values.getLong(i) - result) / (i + 1);
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty FloatArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += (values.getFloat(i) - result) / (i + 1);
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty DoubleArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += (values.getDouble(i) - result) / (i + 1);
            }
            return result;
        }

        @Override
        public String getName() {
            return "mean";
        }

        @Override
        public String getAs() {
            return as;
        }
    }

    private static class CountAggregation implements Aggregation {

        private final String as;

        public CountAggregation() {
            this(null);
        }

        public CountAggregation(String as) {
            this.as = as;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ByteArrayList values) {
            return values.size();
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ShortArrayList values) {
            return values.size();
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty IntegerArrayList values) {
            return values.size();
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty LongArrayList values) {
            return values.size();
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty FloatArrayList values) {
            return values.size();
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty DoubleArrayList values) {
            return values.size();
        }

        @Override
        public String getName() {
            return "count";
        }

        @Override
        public String getAs() {
            return as;
        }
    }

    private static class FirstAggregation implements Aggregation {

        private final String as;

        public FirstAggregation() {
            this(null);
        }

        public FirstAggregation(String as) {
            this.as = as;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ByteArrayList values) {
            return values.getByte(0);
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ShortArrayList values) {
            return values.getShort(0);
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty IntegerArrayList values) {
            return values.getInteger(0);
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty LongArrayList values) {
            return values.getLong(0);
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty FloatArrayList values) {
            return values.getFloat(0);
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty DoubleArrayList values) {
            return values.getDouble(0);
        }

        @Override
        public String getName() {
            return "first";
        }

        @Override
        public String getAs() {
            return as;
        }
    }

    private static class LastAggregation implements Aggregation {

        private final String as;

        public LastAggregation() {
            this(null);
        }

        public LastAggregation(String as) {
            this.as = as;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ByteArrayList values) {
            return values.getByte(values.size() - 1);
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ShortArrayList values) {
            return values.getShort(values.size() - 1);
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty IntegerArrayList values) {
            return values.getInteger(values.size() - 1);
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty LongArrayList values) {
            return values.getLong(values.size() - 1);
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty FloatArrayList values) {
            return values.getFloat(values.size() - 1);
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty DoubleArrayList values) {
            return values.getDouble(values.size() - 1);
        }

        @Override
        public String getName() {
            return "last";
        }

        @Override
        public String getAs() {
            return as;
        }
    }

    private static class SumAggregation implements Aggregation {

        private final String as;

        public SumAggregation() {
            this(null);
        }

        public SumAggregation(String as) {
            this.as = as;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ByteArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += values.getByte(i);
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty ShortArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += values.getShort(i);
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty IntegerArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += values.getInteger(i);
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty LongArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += values.getLong(i);
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty FloatArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += values.getFloat(i);
            }
            return result;
        }

        @Override
        public double aggregate(@Nonnull @NotEmpty DoubleArrayList values) {
            double result = 0;
            for (int i = 0; i < values.size(); i++) {
                result += values.getDouble(i);
            }
            return result;
        }

        @Override
        public String getName() {
            return "sum";
        }

        @Override
        public String getAs() {
            return as;
        }
    }
}
