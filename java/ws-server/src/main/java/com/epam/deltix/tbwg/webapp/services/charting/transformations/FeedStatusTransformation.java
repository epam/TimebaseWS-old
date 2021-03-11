package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.tbwg.messages.FeedStatusMessage;
import com.epam.deltix.tbwg.messages.Message;
import com.epam.deltix.tbwg.messages.SecurityStatus;
import com.epam.deltix.tbwg.messages.SecurityStatusMessage;
import com.epam.deltix.timebase.api.messages.MarketMessageInfo;
import com.epam.deltix.timebase.api.messages.service.FeedStatus;
import com.epam.deltix.timebase.api.messages.service.SecurityFeedStatusMessage;

import java.util.Collections;

public class FeedStatusTransformation extends ChartTransformation<MarketMessageInfo, MarketMessageInfo> {

    private boolean disconnected;

    public FeedStatusTransformation() {
        super(Collections.singletonList(MarketMessageInfo.class), Collections.singletonList(MarketMessageInfo.class));
    }

    @Override
    protected void onMessage(Message message) {
        sendMessage(message);
    }

    @Override
    protected void onNextPoint(MarketMessageInfo marketMessage) {
        if (marketMessage instanceof SecurityStatusMessage) {
            SecurityStatusMessage statusMessage = (SecurityStatusMessage) marketMessage;
            if (statusMessage.getStatus() == SecurityStatus.FEED_DISCONNECTED) {
                disconnected = true;
                sendMessage(
                    new FeedStatusMessage(statusMessage.getTimeStampMs(), statusMessage.getExchangeId(), FeedStatus.NOT_AVAILABLE)
                );
            } else if (statusMessage.getStatus() == SecurityStatus.FEED_CONNECTED) {
                disconnected = false;
                sendMessage(
                    new FeedStatusMessage(statusMessage.getTimeStampMs(), statusMessage.getExchangeId(), FeedStatus.AVAILABLE)
                );
            }
        } else if (marketMessage instanceof SecurityFeedStatusMessage) {
            SecurityFeedStatusMessage statusMessage = (SecurityFeedStatusMessage) marketMessage;
            if (statusMessage.getStatus() == FeedStatus.NOT_AVAILABLE) {
                disconnected = true;
                sendMessage(
                    new FeedStatusMessage(statusMessage.getTimeStampMs(), statusMessage.getExchangeId(), FeedStatus.NOT_AVAILABLE)
                );
            } else {
                disconnected = false;
                sendMessage(
                    new FeedStatusMessage(statusMessage.getTimeStampMs(), statusMessage.getExchangeId(), FeedStatus.AVAILABLE)
                );
            }
        }

        if (!disconnected) {
            sendMessage(marketMessage);
        }
    }
}
