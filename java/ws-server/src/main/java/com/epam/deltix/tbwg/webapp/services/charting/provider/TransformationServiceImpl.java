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
package com.epam.deltix.tbwg.webapp.services.charting.provider;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.tbwg.webapp.model.charting.ChartType;
import com.epam.deltix.tbwg.webapp.services.charting.datasource.MessageSource;
import com.epam.deltix.tbwg.webapp.services.charting.queries.*;
import com.epam.deltix.tbwg.webapp.services.charting.transformations.*;
import com.epam.deltix.timebase.api.messages.BarMessage;
import com.epam.deltix.timebase.api.messages.BestBidOfferMessage;
import com.epam.deltix.timebase.api.messages.L2Message;
import com.epam.deltix.timebase.api.messages.Level2Message;
import com.epam.deltix.timebase.api.messages.universal.PackageHeader;
import io.reactivex.Observable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransformationServiceImpl implements TransformationService {

    private static final Log LOGGER = LogFactory.getLog(TransformationServiceImpl.class);

    private static final long EXTEND_INTERVAL_MS = 60 * 1000;

    private static final double ZOOM_DETAILS_FACTOR = 0.9d;

    private interface TransformationPlanBuilder {
        LinesQueryResult    build(MessageSource source);
    }

    private static class L2PricesPlanBuilder implements TransformationPlanBuilder {

        private final boolean legacy;
        private final AggregationCalculator aggregationCalculator;

        private L2PricesPlanBuilder(int maxPointsPerLine, boolean legacy) {
            this.aggregationCalculator =
                new AggregationCalculatorImpl(maxPointsPerLine, ZOOM_DETAILS_FACTOR);
            this.legacy = legacy;
        }

        @Override
        public LinesQueryResult build(MessageSource source) {
            BookSymbolQueryImpl query = (BookSymbolQueryImpl) source.getQuery();

            long startTime = query.getInterval().getStartTimeMilli();
            long endTime = query.getInterval().getEndTimeMilli();
            long aggregation = aggregationCalculator.getAggregation(query.getInterval());
            long newWindowSize = aggregationCalculator.getNewWindowSize(query.getInterval());

            LinesQueryResult result = new LinesQueryResultImpl(query.getStream() + "[" + query.getSymbol() + "]", query.getInterval());

            Observable<?> inputObservable = source.getInput()
                .takeWhile(x -> x.getTimeStampMs() <= endTime + EXTEND_INTERVAL_MS);

            if (legacy) {
                inputObservable = inputObservable.lift(new LegacyToUniversalTransformation());
            }

            inputObservable = inputObservable.lift(new FeedStatusTransformation());
            inputObservable = inputObservable.lift(new AdaptPeriodicityTransformation(query.getLevelsCount(), aggregation));
            inputObservable = inputObservable.share();

            // Levels
            Observable<?> observable;
            if (aggregation >= 60_000) {
                observable = inputObservable.lift(
                    new UniversalL2SnapshotsToPointsTransformation(query.getSymbol(), query.getLevelsCount(), aggregation)
                );
            } else {
                observable = inputObservable.lift(
                    new UniversalL2ToPointsTransformation(query.getSymbol(), query.getLevelsCount(), aggregation)
                );
            }
            observable = observable.share();

            for (int i = 0; i < query.getLevelsCount(); ++i) {
                LevelPointToDtoTransformation bidLevelTransformation = new LevelPointToDtoTransformation(i, true, startTime, endTime);
                result.getLines().add(
                    new LineResultImpl(
                        "BID[" + i + "]", observable.lift(bidLevelTransformation), aggregation, newWindowSize
                    )
                );

                LevelPointToDtoTransformation askLevelTransformation = new LevelPointToDtoTransformation(i, false, startTime, endTime);
                result.getLines().add(
                    new LineResultImpl(
                        "ASK[" + i + "]", observable.lift(askLevelTransformation), aggregation, newWindowSize
                    )
                );
            }

            // Trades
            result.getLines().add(
                new LineResultImpl(
                    "TRADES",
                    inputObservable.lift(new UniversalToTradeTransformation()).lift(new TradeTransformation(aggregation, startTime)),
                    aggregation, newWindowSize
                )
            );

            return result;
        }

    }

    private static class BarPlanBuilder implements TransformationPlanBuilder {

        private final boolean legacy;
        private final AggregationCalculator aggregationCalculator = new BarsAggregationCalculatorImpl();

        private BarPlanBuilder(boolean legacy) {
            this.legacy = legacy;
        }

        @Override
        public LinesQueryResult build(MessageSource source) {
            BookSymbolQueryImpl query = (BookSymbolQueryImpl) source.getQuery();

            long startTime = query.getInterval().getStartTimeMilli();
            long endTime = query.getInterval().getEndTimeMilli();
            long aggregation = aggregationCalculator.getAggregation(query.getInterval());
            long newWindowSize = aggregationCalculator.getNewWindowSize(query.getInterval());

            LinesQueryResult result = new LinesQueryResultImpl(query.getStream() + "[" + query.getSymbol() + "]", query.getInterval());

            Observable<?> inputObservable = source.getInput()
                .takeWhile(x -> x.getTimeStampMs() <= endTime);

            if (legacy) {
                inputObservable = inputObservable.lift(new LegacyToUniversalTransformation());
            }

            result.getLines().add(
                new LineResultImpl(
                    "BARS",
                    inputObservable.lift(new UniversalToL1Transformation(query.getSymbol()))
                        .lift(new BarAggregationMidptTransformation(query.getSymbol(), aggregation)),
                    aggregation, newWindowSize
                )
            );

            return result;
        }

    }

    private static class BarConversionPlanBuilder implements TransformationPlanBuilder {

        private final AggregationCalculator aggregationCalculator = new BarsAggregationCalculatorImpl();

        @Override
        public LinesQueryResult build(MessageSource source) {
            BookSymbolQueryImpl query = (BookSymbolQueryImpl) source.getQuery();

            long startTime = query.getInterval().getStartTimeMilli();
            long endTime = query.getInterval().getEndTimeMilli();
            long aggregation = aggregationCalculator.getAggregation(query.getInterval());
            long newWindowSize = aggregationCalculator.getNewWindowSize(query.getInterval());

            LinesQueryResult result = new LinesQueryResultImpl(query.getStream() + "[" + query.getSymbol() + "]", query.getInterval());

            Observable<?> inputObservable = source.getInput()
                .takeWhile(x -> x.getTimeStampMs() <= endTime);

            result.getLines().add(
                new LineResultImpl(
                    "BARS",
                    inputObservable.lift(new BarConversionTransformation(aggregation)),
                    aggregation, newWindowSize
                )
            );

            return result;
        }

    }

    public TransformationServiceImpl() {
    }

    @Override
    public List<LinesQueryResult> buildTransformationsPlan(List<MessageSource> sources) {
        List<LinesQueryResult> results = new ArrayList<>();
        for (int i = 0; i < sources.size(); ++i) {
            MessageSource source = sources.get(i);
            LinesQuery query = source.getQuery();
            if (query.getType() == ChartType.PRICES_L2) {
                if (source.getTypes().contains(PackageHeader.CLASS_NAME)) {
                    results.add(new L2PricesPlanBuilder(query.getMaxPointsCount(), false).build(source));
                    continue;
                } else if (source.getTypes().contains(Level2Message.CLASS_NAME) ||
                           source.getTypes().contains(L2Message.CLASS_NAME))
                {
                    results.add(new L2PricesPlanBuilder(query.getMaxPointsCount(), true).build(source));
                    continue;
                }
            }
            if (query.getType() == ChartType.BARS) {
                if (source.getTypes().contains(PackageHeader.CLASS_NAME)) {
                    results.add(new BarPlanBuilder(false).build(source));
                    continue;
                } else if (source.getTypes().contains(BestBidOfferMessage.CLASS_NAME)) {
                    results.add(new BarPlanBuilder(true).build(source));
                    continue;
                } else if (source.getTypes().contains(BarMessage.CLASS_NAME)) {
                    results.add(new BarConversionPlanBuilder().build(source));
                    continue;
                }
            }

            throw new IllegalArgumentException("Unknown type of getQuery");
        }

        return results;
    }

}
