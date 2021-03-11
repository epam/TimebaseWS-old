package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.quoteflow.LegacyToUniversalConverter;
import com.epam.deltix.tbwg.messages.Message;
import com.epam.deltix.timebase.api.messages.MarketMessageInfo;
import com.epam.deltix.timebase.api.messages.universal.PackageHeader;

import java.util.Collections;

/**
 * The transformation converts legacy market message (bbo, l2, level2, trade) to package header.
 */
public class LegacyToUniversalTransformation extends ChartTransformation<PackageHeader, MarketMessageInfo> {

    private final LegacyToUniversalConverter converter = new LegacyToUniversalConverter();

    public LegacyToUniversalTransformation() {
        super(Collections.singletonList(MarketMessageInfo.class), Collections.singletonList(PackageHeader.class));

        converter.addOnEndTransformationListener(this::converted);
    }

    @Override
    protected void onMessage(Message message) {
        sendMessage(message);
    }

    @Override
    protected void onNextPoint(MarketMessageInfo message) {
        if (message instanceof PackageHeader) {
            sendMessage(message);
        } else {
            converter.sendPackage(message);
        }
    }

    private void converted(MarketMessageInfo message) {
        if (message instanceof PackageHeader) {
            sendMessage(message);
        }
    }

}
