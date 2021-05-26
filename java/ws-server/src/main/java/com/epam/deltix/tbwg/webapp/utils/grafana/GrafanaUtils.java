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
package com.epam.deltix.tbwg.webapp.utils.grafana;


import com.epam.deltix.tbwg.webapp.model.grafana.TimeSeriesEntry;
import com.epam.deltix.grafana.model.DataFrame;
import com.epam.deltix.grafana.model.fields.Column;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GrafanaUtils {

    public static List<TimeSeriesEntry> convert(DataFrame dataFrame) {
        Column timestamp = dataFrame.getFields().stream()
                .filter(column -> column.name().equalsIgnoreCase("timestamp"))
                .findFirst().orElse(null);
        if (timestamp == null) {
            return Collections.emptyList();
        }
        return dataFrame.getFields().stream()
                .filter(column -> !column.name().equalsIgnoreCase("timestamp"))
                .map(column -> {
                    TimeSeriesEntry entry = new TimeSeriesEntry(column.name());
                    for (int i = 0; i < column.values().size(); i++) {
                        entry.datapoints.add(new Object[]{column.values().get(i), timestamp.values().get(i)});
                    }
                    return entry;
                })
                .collect(Collectors.toList());
    }

}
