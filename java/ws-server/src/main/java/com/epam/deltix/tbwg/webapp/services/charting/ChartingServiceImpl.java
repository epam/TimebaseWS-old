package com.epam.deltix.tbwg.webapp.services.charting;

import com.epam.deltix.tbwg.webapp.model.charting.ChartType;
import com.epam.deltix.tbwg.webapp.model.charting.ChartingFrameDef;
import com.epam.deltix.tbwg.webapp.model.charting.ChartingLineDef;
import com.epam.deltix.tbwg.webapp.model.charting.TimeSeriesEntry;
import com.epam.deltix.tbwg.webapp.model.charting.line.BarElementDef;
import com.epam.deltix.tbwg.webapp.model.charting.line.LineElement;
import com.epam.deltix.tbwg.webapp.model.charting.line.LinePointDef;
import com.epam.deltix.tbwg.webapp.model.charting.line.TagElementDef;
import com.epam.deltix.tbwg.webapp.services.charting.provider.LinesProvider;
import com.epam.deltix.tbwg.webapp.services.charting.queries.BookSymbolQueryImpl;
import com.epam.deltix.tbwg.webapp.services.charting.queries.ChartingResult;
import com.epam.deltix.tbwg.webapp.services.charting.queries.LinesQueryResult;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChartingServiceImpl implements ChartingService {

    private final LinesProvider provider;

    @Autowired
    public ChartingServiceImpl(LinesProvider provider) {
        this.provider = provider;
    }

    @Override
    public List<ChartingFrameDef> getData(String stream, List<String> symbols, ChartType type,
                                          TimeInterval interval, int maxPoints, int levels)
    {
        ChartingResult result = provider.getLines(
            symbols.stream()
                .map(s -> new BookSymbolQueryImpl(stream, s, type, interval, maxPoints, levels))
                .collect(Collectors.toList())
        );

        return buildChartingFrames(result);
    }

    private List<ChartingFrameDef> buildChartingFrames(ChartingResult result) {
        List<ChartingFrameDef> frames = new ArrayList<>();

        result.results().forEach(chartResult -> {
            Map<String, ChartingLineDef> lines = new HashMap<>();
            chartResult.getLines().forEach(lineResult -> {
                List<LineElement> elements = new ArrayList<>();
                lineResult.getPoints().subscribe(message -> {
                    if (message instanceof LineElement) {
                        elements.add((LineElement) message);
                    }
                });

                lines.put(
                    lineResult.getName(),
                    new ChartingLineDef(lineResult.getAggregation(), lineResult.getNewWindowSize(), elements)
                );
            });

            frames.add(new ChartingFrameDef(chartResult.getName(), lines, chartResult.getInterval()));
        });

        result.run();

        // todo: remove this temp code to store bars for testing
//        ChartingFrameDef frameDef = frames.get(0);
//        ChartingLineDef line = frameDef.getLines().get("BARS");
//        DXTickStream stream  = timebase.getStream("test_bars");
//        try (TickLoader loader = stream.createLoader(new LoadingOptions(false))) {
//            for (LineElement element : line.getPoints()) {
//                if (element instanceof BarElementDef) {
//                    BarElementDef bar = (BarElementDef) element;
//
//                    BarMessage barMessage = new BarMessage();
//                    barMessage.setOpen(Double.valueOf(bar.getOpen()));
//                    barMessage.setClose(Double.valueOf(bar.getClose()));
//                    barMessage.setLow(Double.valueOf(bar.getLow()));
//                    barMessage.setHigh(Double.valueOf(bar.getHigh()));
//                    barMessage.setTimeStampMs(bar.getTime());
//
//                    loader.send(barMessage);
//                }
//            }
//        }

        return frames;
    }
}
