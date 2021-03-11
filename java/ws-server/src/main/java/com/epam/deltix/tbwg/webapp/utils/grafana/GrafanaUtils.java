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
