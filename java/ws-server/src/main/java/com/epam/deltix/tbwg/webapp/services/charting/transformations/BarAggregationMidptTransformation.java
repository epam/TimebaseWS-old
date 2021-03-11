package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.tbwg.webapp.model.charting.line.BarElementDef;
import com.epam.deltix.tbwg.messages.Message;
import com.epam.deltix.tbwg.messages.OrderBookLinePoint;
import com.epam.deltix.timebase.api.messages.QuoteSide;

import java.util.Collections;

/**
 * The transformation aggregates bars from l1 data and converts into dto.
 */
public class BarAggregationMidptTransformation extends ChartTransformation<BarElementDef, OrderBookLinePoint> {

    private final long periodicity;

    private long timestamp = Long.MIN_VALUE;
    private long open = Decimal64Utils.NULL;
    private long close = Decimal64Utils.NULL;
    private long low = Decimal64Utils.NULL;
    private long high = Decimal64Utils.NULL;
    private long volume = Decimal64Utils.NULL;

    private long bidPrice = Decimal64Utils.NULL;
    private long askPrice = Decimal64Utils.NULL;

    public BarAggregationMidptTransformation(String symbol, long periodicity) {
        super(Collections.singletonList(OrderBookLinePoint.class), Collections.singletonList(BarElementDef.class));

        this.periodicity = periodicity;
    }

    @Override
    protected void onMessage(Message message) {
        sendMessage(message);
    }

    @Override
    protected void onNextPoint(OrderBookLinePoint point) {
        if (point.getLevel() > 0) {
            return;
        }

        flush(point.getTimeStampMs());

        if (point.getSide() == QuoteSide.BID) {
            bidPrice = point.getValue();
        } else if (point.getSide() == QuoteSide.ASK) {
            askPrice = point.getValue();
        }

        if (bidPrice != Decimal64Utils.NULL && askPrice != Decimal64Utils.NULL) {
            update(point.getTimeStampMs(), Decimal64Utils.divide(Decimal64Utils.add(bidPrice, askPrice), Decimal64Utils.TWO), Decimal64Utils.ZERO);
        }
    }

    private void update(long timestamp, long value, long size) {
        if (this.timestamp == Long.MIN_VALUE) {
            this.timestamp = timestamp % periodicity == 0 ? timestamp : timestamp + periodicity - (timestamp % periodicity);
            open = value;
            close = value;
            low = value;
            high = value;
            volume = size;
        } else {
            volume = Decimal64Utils.add(volume, size);
            if (Decimal64Utils.isGreater(value, high)) {
                high = value;
            }
            if (Decimal64Utils.isLess(value, low)) {
                low = value;
            }
            close = value;
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
        bar.setOpen(Decimal64Utils.toString(open));
        bar.setClose(Decimal64Utils.toString(close));
        bar.setLow(Decimal64Utils.toString(low));
        bar.setHigh(Decimal64Utils.toString(high));
        bar.setVolume("0");

        sendMessage(bar);
    }

    private void clear() {
        this.timestamp = Long.MIN_VALUE;
        open = Decimal64Utils.NULL;
        close = Decimal64Utils.NULL;
        low = Decimal64Utils.NULL;
        high = Decimal64Utils.NULL;
        volume = Decimal64Utils.NULL;

        bidPrice = Decimal64Utils.NULL;
        askPrice = Decimal64Utils.NULL;
    }

}