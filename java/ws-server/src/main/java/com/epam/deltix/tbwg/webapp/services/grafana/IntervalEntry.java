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

import com.epam.deltix.anvil.util.Reusable;
import com.epam.deltix.util.collections.generated.ObjectToDoubleHashMap;

public class IntervalEntry implements Reusable {

    private final ObjectToDoubleHashMap<String> values = new ObjectToDoubleHashMap<>();

    private long timestamp = Long.MIN_VALUE;
    @Override
    public void reuse() {
        values.clear();
        timestamp = Long.MIN_VALUE;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ObjectToDoubleHashMap<String> getValues() {
        return values;
    }

    public void put(String field, double value) {
        values.put(field, value);
    }
}
