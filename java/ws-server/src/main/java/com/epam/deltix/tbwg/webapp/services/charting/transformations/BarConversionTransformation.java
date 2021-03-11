package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.tbwg.webapp.model.charting.line.BarElementDef;
import com.epam.deltix.tbwg.messages.Message;
import com.epam.deltix.timebase.api.messages.BarMessage;

import java.util.Collections;

/**
 * The transformation aggregates bars from another bars and converts into dto.
 */
public class BarConversionTransformation extends ChartTransformation<BarElementDef, BarMessage> {

    private final long periodicity;

    private long timestamp = Long.MIN_VALUE;
    private double open = Double.NaN;
    private double close = Double.NaN;
    private double low = Double.NaN;
    private double high = Double.NaN;
    private double volume = Double.NaN;

    public BarConversionTransformation(long periodicity) {
        super(Collections.singletonList(BarMessage.class), Collections.singletonList(BarElementDef.class));

        this.periodicity = periodicity;
    }

    @Override
    protected void onMessage(Message message) {
        sendMessage(message);
    }

    @Override
    protected void onNextPoint(BarMessage barMessage) {
        flush(barMessage.getTimeStampMs());

        if (this.timestamp == Long.MIN_VALUE) {
            long barTimestamp = barMessage.getTimeStampMs();
            this.timestamp = barTimestamp % periodicity == 0 ? barTimestamp : barTimestamp + periodicity - (barTimestamp % periodicity);
            this.open = barMessage.getOpen();
            this.close = barMessage.getClose();
            this.high = barMessage.getHigh();
            this.low = barMessage.getLow();
            this.volume = barMessage.getVolume();
        } else {
            this.close = barMessage.getClose();
            this.high = Math.max(barMessage.getHigh(), high);
            this.low = Math.min(barMessage.getLow(), low);
            this.volume += barMessage.getVolume();
        }
    }

    private void flush(long timestamp) {
        if (this.timestamp == Long.MIN_VALUE || timestamp <= this.timestamp) {
            return;
        }

        send();
        clear();
    }

    private void send() {
        BarElementDef bar = new BarElementDef();
        bar.setTime(timestamp);
        bar.setOpen(Decimal64Utils.toString(Decimal64Utils.fromDouble(open)));
        bar.setClose(Decimal64Utils.toString(Decimal64Utils.fromDouble(close)));
        bar.setLow(Decimal64Utils.toString(Decimal64Utils.fromDouble(low)));
        bar.setHigh(Decimal64Utils.toString(Decimal64Utils.fromDouble(high)));
        bar.setVolume(Decimal64Utils.toString(Decimal64Utils.fromDouble(volume)));

        sendMessage(bar);
    }

    private void clear() {
        timestamp = Long.MIN_VALUE;
        open = Double.NaN;
        close = Double.NaN;
        low = Double.NaN;
        high = Double.NaN;
        volume = Double.NaN;
    }

}
