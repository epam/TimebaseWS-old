package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.tbwg.messages.ExecutionTag;
import com.epam.deltix.tbwg.messages.Message;
import com.epam.deltix.timebase.api.messages.AggressorSide;
import com.epam.deltix.timebase.api.messages.MarketMessageInfo;
import com.epam.deltix.timebase.api.messages.QuoteSide;
import com.epam.deltix.timebase.api.messages.universal.PackageHeader;
import com.epam.deltix.timebase.api.messages.universal.TradeEntryInfo;

import java.util.Collections;

/**
 * The transformation filters package headers and leaves only trades.
 */
public class UniversalToTradeTransformation extends ChartTransformation<ExecutionTag, MarketMessageInfo> {

    private final ExecutionTag tradeTag = new ExecutionTag();

    public UniversalToTradeTransformation() {
        super(Collections.singletonList(PackageHeader.class), Collections.singletonList(ExecutionTag.class));
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
                if (entry instanceof TradeEntryInfo) {
                    flushTrade(message.getTimeStampMs(), (TradeEntryInfo) entry);
                }
            });
        }
    }

    private void flushTrade(long timestamp, TradeEntryInfo trade) {
        tradeTag.setTimeStampMs(timestamp);
        tradeTag.setValue(trade.getPrice());
        tradeTag.setPrice(trade.getPrice());
        tradeTag.setSize(trade.getSize());
        tradeTag.setSide(trade.getSide() == AggressorSide.BUY ? QuoteSide.BID : QuoteSide.ASK);
        sendMessage(tradeTag);
    }
}
