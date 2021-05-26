/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
