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
import com.epam.deltix.tbwg.webapp.config.ChartingConfiguration;
import com.epam.deltix.tbwg.webapp.services.charting.datasource.DataSource;
import com.epam.deltix.tbwg.webapp.services.charting.datasource.MessageSource;
import com.epam.deltix.tbwg.webapp.services.charting.queries.*;
import com.epam.deltix.timebase.api.rx.ReactiveMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class TimeBaseLinesProvider implements LinesProvider {

    private static final Log LOGGER = LogFactory.getLog(TimeBaseLinesProvider.class);

    private final ChartingConfiguration config;
    private final DataSource dataSource;
    private final TransformationService transformationService;
    private final ThreadPoolTaskExecutor threadPool;

    @Autowired
    public TimeBaseLinesProvider(ChartingConfiguration config,
                                 DataSource dataSource,
                                 TransformationService transformationService)
    {
        this.config = config;
        this.dataSource = dataSource;
        this.transformationService = transformationService;

        threadPool = new ThreadPoolTaskExecutor();
        threadPool.setCorePoolSize(config.getCorePoolSize());
        threadPool.setMaxPoolSize(config.getMaxPoolSize());
        threadPool.initialize();
    }

    @Override
    public ChartingResult getLines(List<LinesQuery> queries) {
        List<MessageSource> sources = dataSource.buildSources(queries);
        List<LinesQueryResult> transformations = transformationService.buildTransformationsPlan(sources);

        return new ChartingResultImpl(
            transformations,
            () -> {
                long startTime = System.currentTimeMillis();
                runAsync(sources.stream().map(MessageSource::getSource).collect(Collectors.toSet()));
                LOGGER.info()
                    .append("Queries: ").append(Arrays.toString(queries.toArray(new LinesQuery[0])))
                    .append(", Transformation time: ").append(System.currentTimeMillis() - startTime).commit();
            }
        );
    }

    private void runAsync(final Set<ReactiveMessageSource> reactiveMessageSources) {
        final List<Future<?>> forwardFutures = reactiveMessageSources.stream().map(threadPool::submit).collect(Collectors.toList());
        for (final Future<?> future : forwardFutures) {
            try {
                future.get(config.getQueryTimeoutSec(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // do nothing
            } catch (ExecutionException e) {
                throw new IllegalStateException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException("Long response");
            }
        }
    }

}
