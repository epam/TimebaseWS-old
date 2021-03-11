package com.epam.deltix.tbwg.webapp.services.charting;

import com.epam.deltix.tbwg.webapp.model.charting.ChartType;
import com.epam.deltix.tbwg.webapp.model.charting.ChartingFrameDef;
import com.epam.deltix.tbwg.webapp.model.charting.TimeSeriesEntry;

import java.util.List;

public interface ChartingService {

    List<ChartingFrameDef>          getData(String streamKey, List<String> symbols, ChartType type,
                                            TimeInterval interval, int maxPoints, int levels);

}
