package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.tbwg.webapp.model.charting.line.ExecutionTagElementDef;
import com.epam.deltix.tbwg.webapp.model.charting.line.TagType;
import com.epam.deltix.tbwg.messages.ChangePeriodicity;
import com.epam.deltix.tbwg.messages.ExecutionTag;
import com.epam.deltix.tbwg.messages.Message;

import java.util.Collections;

public class TradeTransformation extends ChartTransformation<ExecutionTagElementDef, ExecutionTag> {

    private final PeriodicityFilter filter;
    private final long startTime;
    private ExecutionTagElementDef element = new ExecutionTagElementDef();

    public TradeTransformation(long periodicity, long startTime) {
        super(Collections.singletonList(ExecutionTag.class), Collections.singletonList(ExecutionTagElementDef.class));

        this.startTime = startTime;
        this.filter = new PeriodicityFilter(periodicity);
    }

    @Override
    protected void onMessage(Message message) {
        if (message instanceof ChangePeriodicity) {
            filter.setPeriodicity(((ChangePeriodicity) message).getPeriodicity());
        }

        sendMessage(message);
    }

    @Override
    protected void onNextPoint(ExecutionTag trade) {
        if (trade.getTimeStampMs() < startTime) {
            return;
        }

        if (filter.test(trade)) {
            element.setTime(trade.getTimeStampMs());
            element.setTagType(TagType.EXECUTION);
            element.setValue(Decimal64Utils.toString(trade.getValue()));
            element.setPrice(Decimal64Utils.toString(trade.getPrice()));
            element.setSize(Decimal64Utils.toString(trade.getSize()));
            element.setSide(trade.getSide());
            sendMessage(element);

            element = new ExecutionTagElementDef();
        }
    }
}
