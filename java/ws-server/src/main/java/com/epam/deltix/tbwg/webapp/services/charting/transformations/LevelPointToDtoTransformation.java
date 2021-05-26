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

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.tbwg.webapp.model.charting.line.LinePointDef;
import com.epam.deltix.tbwg.messages.OrderBookLinePoint;
import com.epam.deltix.tbwg.messages.Message;
import com.epam.deltix.timebase.api.messages.QuoteSide;

import java.util.Collections;

/**
 * The transformation removes the same adjacent time series line points and converts to output dto.
 */
public class LevelPointToDtoTransformation extends ChartTransformation<LinePointDef, OrderBookLinePoint> {

    private final int level;
    private final QuoteSide side;
    private final long startTime;
    private final long endTime;

    private boolean intervalStarted;
    private boolean intervalEnded;
    private long lastPrice = Decimal64Utils.NaN;
    private long lastTimestamp = Long.MIN_VALUE;

    public LevelPointToDtoTransformation(int level, boolean bid, long startTime, long endTime) {
        super(Collections.singletonList(OrderBookLinePoint.class), Collections.singletonList(LinePointDef.class));

        this.level = level;
        this.side = bid ? QuoteSide.BID : QuoteSide.ASK;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    protected void onMessage(Message message) {
        sendMessage(message);
    }

    @Override
    protected void onNextPoint(OrderBookLinePoint point) {
        if (intervalEnded) {
            return;
        }

        if (point.getLevel() == level && side == point.getSide()) {
            if (!intervalStarted) {
                if (point.getTimeStampMs() >= startTime) {
                    intervalStarted = true;
                    if (lastPrice != Decimal64Utils.NaN) {
                        sendPoint(startTime, lastPrice);
                    }
                } else {
                    lastPrice = point.getValue();
                }
            }

            long timestamp = point.getTimeStampMs();
            if (timestamp > endTime) {
                intervalEnded = true;
                return;
            }

            if (!Decimal64Utils.equals(lastPrice, point.getValue())){
                sendPoint(lastTimestamp = timestamp, lastPrice = point.getValue());
            }
        }
    }

    @Override
    protected void onComplete() {
        if (lastTimestamp != Long.MIN_VALUE && lastTimestamp != endTime) {
            sendPoint(endTime, lastPrice);
        }
    }

    private void sendPoint(long timestamp, long value) {
        LinePointDef linePoint = new LinePointDef();
        linePoint.setTime(timestamp);
        linePoint.setValue(Decimal64Utils.toString(value));
        sendMessage(linePoint);
    }
}
