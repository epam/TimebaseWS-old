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

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.tbwg.webapp.model.grafana.time.TimeRange;
import com.epam.deltix.timebase.messages.InstrumentMessage;

public interface Aggregator {

    default IntervalEntry nextInterval(MessageSource<InstrumentMessage> messageSource) {
        IntervalEntry intervalEntry = new IntervalEntry();
        if (nextInterval(messageSource, intervalEntry)) {
            return intervalEntry;
        } else {
            return null;
        }
    }

    boolean nextInterval(MessageSource<InstrumentMessage> messageSource, IntervalEntry intervalEntry);

    static long calculateStep(long startTime, long endTime, long intervals) {
        long d = intervals - (endTime - startTime) % intervals;
        return (endTime - startTime + d) / intervals;
    }

    static long calculateStep(TimeRange range, long intervals) {
        return calculateStep(range.getFrom().toEpochMilli(), range.getTo().toEpochMilli(), intervals);
    }
}
