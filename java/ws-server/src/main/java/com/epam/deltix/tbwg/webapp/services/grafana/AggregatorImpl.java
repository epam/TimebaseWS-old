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
package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.util.HashMap;
import java.util.Map;

public class AggregatorImpl implements Aggregator {

    private final long startTime;
    private final long endTime;
    private final long step;
    private final Aggregation function;
    private final NumericDecoder decoder = new NumericDecoder();
    private final Map<String, NumericListDelegate> rawValues = new HashMap<>();

    private long currentFirst = Long.MIN_VALUE;
    private long currentLast = Long.MIN_VALUE;
    private long current = Long.MIN_VALUE;

    private boolean cursorFinished = false;

    public AggregatorImpl(long startTime, long endTime, long intervals, Aggregation function) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.function = function;
        this.step = Aggregator.calculateStep(startTime, endTime, intervals);
    }

    private boolean next() {
        if (cursorFinished) {
            return false;
        }
        if (currentFirst == Long.MIN_VALUE) {
            currentFirst = startTime;
        } else {
            currentFirst += step;
        }
        if (currentFirst > endTime) {
            return false;
        }
        currentLast = Math.min(currentFirst + step, endTime);
        current = currentLast;
        return true;
    }

    @Override
    public boolean nextInterval(MessageSource<InstrumentMessage> messageSource, IntervalEntry intervalEntry) {
        intervalEntry.reuse();
        intervalEntry.setTimestamp(current);
        if (next()) {
            rawValues.clear();
            do {
                if (messageSource.getMessage() != null && messageSource.getMessage().getTimeStampMs() != Long.MIN_VALUE) {
                    RawMessage rawMessage = (RawMessage) messageSource.getMessage();
                    if (rawMessage.getTimeStampMs() >= currentFirst && rawMessage.getTimeStampMs() < currentLast) {
                        decoder.decode(rawMessage, rawValues);
                    } else if (rawMessage.getTimeStampMs() >= currentLast) {
                        break;
                    }
                }
                if (!messageSource.next()) {
                    cursorFinished = true;
                }
            } while (!cursorFinished && messageSource.getMessage().getTimeStampMs() < currentLast);
            rawValues.forEach((field, values) -> {
                if (!values.getList().isEmpty()) {
                    intervalEntry.put(field, function.aggregate(values.getList()));
                }
            });
            return true;
        } else {
            return false;
        }
    }
}
