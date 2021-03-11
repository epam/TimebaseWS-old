package com.epam.deltix.grafana.stats;

import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.computations.base.exc.RecordValidationException;
import com.epam.deltix.computations.data.MutableGenericRecordImpl;
import com.epam.deltix.computations.data.base.GenericRecord;
import com.epam.deltix.computations.data.base.MutableGenericRecord;
import com.epam.deltix.computations.data.numeric.MutableDoubleValue;
import com.epam.deltix.grafana.base.Aggregation;
import com.epam.deltix.grafana.data.NumericField;
import com.epam.deltix.grafana.model.fields.Field;
import rtmath.finanalysis.indicators.Bollinger;

import java.util.Collection;
import java.util.List;

public class BollingerBands implements Aggregation {

    public static final String FIELD = "field";
    public static final String COUNT = "count";
    public static final String FACTOR = "factor";
    public static final String PERIOD = "periodMs";

    public static final String LOWER_BAND = "lowerBand";
    public static final String MIDDLE_BAND = "middleBand";
    public static final String UPPER_BAND = "upperBand";
    public static final String PERCENT_B = "percentB";
    public static final String BAND_WIDTH = "bandWidth";

    private final Bollinger bollinger;

    private final String fieldName;
    private final List<Field> resultFields = new ObjectArrayList<>();
    private final MutableGenericRecord resultRecord = new MutableGenericRecordImpl();
    private final MutableDoubleValue lowerBandValue = new MutableDoubleValue();
    private final MutableDoubleValue middleBandValue = new MutableDoubleValue();
    private final MutableDoubleValue upperBandValue = new MutableDoubleValue();
    private final MutableDoubleValue percentBValue = new MutableDoubleValue();
    private final MutableDoubleValue bandWidthValue = new MutableDoubleValue();


    public BollingerBands(String fieldName, int count, double factor,
                          String lowerBand, String middleBand, String upperBand, String percentB, String bandWidth) {
        this.fieldName = fieldName;
        bollinger = new Bollinger(count, factor);

        Field lowerBandField = new NumericField(lowerBand);
        Field middleBandField = new NumericField(middleBand);
        Field upperBandField = new NumericField(upperBand);
        Field percentBField = new NumericField(percentB);
        Field bandWidthField = new NumericField(bandWidth);
        resultFields.add(lowerBandField);
        resultFields.add(middleBandField);
        resultFields.add(upperBandField);
        resultFields.add(percentBField);
        resultFields.add(bandWidthField);

        resultRecord.set(lowerBand, lowerBandValue);
        resultRecord.set(middleBand, middleBandValue);
        resultRecord.set(upperBand, upperBandValue);
        resultRecord.set(percentB, percentBValue);
        resultRecord.set(bandWidth, bandWidthValue);
    }

    public BollingerBands(String fieldName, long period, double factor,
                          String lowerBand, String middleBand, String upperBand, String percentB, String bandWidth) {
        this.fieldName = fieldName;
        bollinger = new Bollinger(period, factor);

        Field lowerBandField = new NumericField(lowerBand);
        Field middleBandField = new NumericField(middleBand);
        Field upperBandField = new NumericField(upperBand);
        Field percentBField = new NumericField(percentB);
        Field bandWidthField = new NumericField(bandWidth);
        resultFields.add(lowerBandField);
        resultFields.add(middleBandField);
        resultFields.add(upperBandField);
        resultFields.add(percentBField);
        resultFields.add(bandWidthField);

        resultRecord.set(lowerBand, lowerBandValue);
        resultRecord.set(middleBand, middleBandValue);
        resultRecord.set(upperBand, upperBandValue);
        resultRecord.set(percentB, percentBValue);
        resultRecord.set(bandWidth, bandWidthValue);
    }

    @Override
    public Collection<Field> fields() {
        return resultFields;
    }

    @Override
    public boolean add(GenericRecord record) throws RecordValidationException {
        if (record.containsNonNull(fieldName)) {
            bollinger.add(record.getValue(fieldName).doubleValue(), record.timestamp());
            if (bollinger.ready) {
                lowerBandValue.set(bollinger.lowerBand);
                middleBandValue.set(bollinger.middleBand);
                upperBandValue.set(bollinger.upperBand);
                percentBValue.set(bollinger.percentB);
                bandWidthValue.set(bollinger.bandWidth);
                resultRecord.setTimestamp(record.timestamp());
                return true;
            }
        }
        return false;
    }

    @Override
    public GenericRecord record(long timestamp) {
        return resultRecord;
    }

    @Override
    public boolean isValid(GenericRecord record) {
        return true;
    }
}
