package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.tbwg.messages.Message;
import com.epam.deltix.tbwg.messages.OrderBookLinePoint;
import com.epam.deltix.timebase.api.messages.MarketMessageInfo;
import com.epam.deltix.timebase.api.messages.universal.L1Entry;
import com.epam.deltix.timebase.api.messages.universal.PackageHeader;

import java.util.Collections;

/**
 * The transformation filters package headers and leaves only l1 entries.
 */
public class UniversalToL1Transformation extends ChartTransformation<OrderBookLinePoint, MarketMessageInfo> {

    private final OrderBookLinePoint outputPoint = new OrderBookLinePoint();

    public UniversalToL1Transformation(String symbol) {
        super(Collections.singletonList(PackageHeader.class), Collections.singletonList(OrderBookLinePoint.class));

        this.outputPoint.setSymbol(symbol);
        this.outputPoint.setLevel(0);
    }

    @Override
    protected void onMessage(Message message) {
        sendMessage(message);
    }

    @Override
    protected void onNextPoint(MarketMessageInfo marketMessage) {
        if (marketMessage instanceof PackageHeader) {
            PackageHeader message = (PackageHeader) marketMessage;
            message.getEntries().forEach(entry -> {
                if (entry instanceof L1Entry) {
                    flushEntry(message.getTimeStampMs(), (L1Entry) entry);
                }
            });
        }
    }

    private void flushEntry(long timestamp, L1Entry entry) {
        outputPoint.setTimeStampMs(timestamp);
        outputPoint.setValue(entry.getPrice());
        outputPoint.setVolume(entry.getSize());
        outputPoint.setSide(entry.getSide());
        sendMessage(outputPoint);
    }
}
