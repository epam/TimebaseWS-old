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
package com.epam.deltix.tbwg.webapp.services.charting.datasource;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.tbwg.webapp.model.charting.ChartType;
import com.epam.deltix.tbwg.webapp.services.TimebaseServiceImpl;
import com.epam.deltix.tbwg.webapp.services.charting.queries.LinesQuery;
import com.epam.deltix.tbwg.webapp.services.charting.queries.SymbolQuery;
import com.epam.deltix.timebase.api.messages.*;
import com.epam.deltix.timebase.api.messages.service.SecurityFeedStatusMessage;
import com.epam.deltix.timebase.api.messages.universal.PackageHeader;
import com.epam.deltix.timebase.api.rx.ReactiveMessageSource;
import com.epam.deltix.timebase.messages.IdentityKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TimeBaseDataSource implements DataSource {

    private final long PREFETCH_INTERVAL_MS = 60 * 1000;

    private final TimebaseServiceImpl timebase;

    @Autowired
    public TimeBaseDataSource(TimebaseServiceImpl timebase) {
        this.timebase = timebase;
    }

    @Override
    public List<MessageSource> buildSources(List<LinesQuery> queries) {
        List<MessageSource> sources = new ArrayList<>();
        getBuilders(queries).forEach(b -> {
            ReactiveMessageSource source = b.builder.build();
            for (int i = 0; i < b.infos.size(); ++i) {
                sources.add(
                    new MessageSourceImpl(
                        b.infos.get(i).query,
                        source,
                        source.getObservables().get(i),
                        b.infos.get(i).inputTypes
                    )
                );
            }
        });

        return sources;
    }

    private List<SourceBuilder> getBuilders(List<LinesQuery> queries) {
        Map<String, SourceBuilder> builders = new HashMap<>();

        long time = Long.MAX_VALUE;
        for (int i = 0; i < queries.size(); ++i) {
            LinesQuery query = queries.get(i);
            if (query instanceof SymbolQuery) {
                SymbolQuery symbolQuery = (SymbolQuery) query;

                DXTickStream stream = chooseStream(symbolQuery);
                if (stream == null) {
                    throw new IllegalArgumentException("Can't find stream " + symbolQuery.getStream());
                }

                DXTickDB db = stream.getDB();
                SourceBuilder builder = builders.computeIfAbsent(db.getId(), k -> new SourceBuilder(ReactiveMessageSource.builder(db)));

                long currentStartTime = symbolQuery.getInterval().getStartTimeMilli();
                if (currentStartTime < time) {
                    time = currentStartTime;
                }

                Set<String> types = getInputTypes(stream, symbolQuery);
                IdentityKey instrument = findInstrument(stream, symbolQuery.getSymbol());

                builder.builder.time(time - PREFETCH_INTERVAL_MS);
                builder.builder.typeLoader(MarketDataTypeLoader.TYPE_LOADER);
                builder.builder.streams(stream);
                builder.builder.symbols(instrument);
                builder.builder.types(types);
                builder.builder.addGroup();

                builder.infos.add(new SourceInfo(query, types));
            }
        }

        return new ArrayList<>(builders.values());
    }

    private IdentityKey findInstrument(DXTickStream stream, String symbol) {
        IdentityKey[] instruments = stream.listEntities();
        for (int i = 0; i < instruments.length; ++i) {
            if (instruments[i].getSymbol().toString().equals(symbol)) {
                return instruments[i];
            }
        }

        throw new IllegalArgumentException("Can't find symbol '" + symbol + "' in stream '" + stream.getKey() + "'");
    }

    private Set<String> getInputTypes(DXTickStream stream, SymbolQuery symbolQuery) {
        RecordClassDescriptor[] descriptors = stream.getTypes();

        if (symbolQuery.getType() == ChartType.PRICES_L2) {
            if (mayContainSubclasses(stream, PackageHeader.class)) {
                Set<String> descriptorsSet = getDescriptors(descriptors, PackageHeader.class, SecurityFeedStatusMessage.class);
                descriptorsSet.add(MarketDataTypeLoader.SECURITY_STATUS_CLASS);
                return descriptorsSet;
            }
            if (mayContainSubclasses(stream, Level2Message.class) ||
                mayContainSubclasses(stream, L2Message.class))
            {
                return getDescriptors(descriptors, Level2Message.class, L2Message.class, L2SnapshotMessage.class, TradeMessage.class);
            }
        }
        if (symbolQuery.getType() == ChartType.BARS) {
            if (mayContainSubclasses(stream, PackageHeader.class)) {
                Set<String> descriptorsSet = getDescriptors(descriptors, PackageHeader.class, SecurityFeedStatusMessage.class);
                descriptorsSet.add(MarketDataTypeLoader.SECURITY_STATUS_CLASS);
                return descriptorsSet;
            }
            if (mayContainBBOMessages(stream)) {
                return getDescriptors(descriptors, BestBidOfferMessage.class);
            }
            if (mayContainBarMessages(stream)) {
                return getDescriptors(descriptors, BarMessage.class);
            }
        }

        throw new IllegalArgumentException("Stream " + stream.getKey() + " type mismatch with chart type " + symbolQuery.getType());
    }

    private Set<String> getDescriptors(RecordClassDescriptor[] descriptors, Class<?>... classes) {
        Set<String> foundClasses = new HashSet<>();
        for (int i = 0; i < classes.length; ++i) {
            String className = ClassDescriptor.getClassNameWithAssembly(classes[i]);
            foundClasses.add(className);
            for (int j = 0; j < descriptors.length; ++j) {
                RecordClassDescriptor descriptor = descriptors[j];
                if (descriptor.isConvertibleTo(className)) {
                    foundClasses.add(descriptor.getName());
                }
            }
        }

        return foundClasses;
    }

    private DXTickStream chooseStream(SymbolQuery query) {
        //todo: choose stream between 'main' tb and 'cache' tb
        return timebase.getStream(query.getStream());
    }

    private static class SourceBuilder {
        private final ReactiveMessageSource.Builder builder;
        private final List<SourceInfo> infos = new ArrayList<>();

        private SourceBuilder(ReactiveMessageSource.Builder builder) {
            this.builder = builder;
        }
    }

    private static class SourceInfo {
        private LinesQuery query;
        private Set<String> inputTypes;

        public SourceInfo(LinesQuery query, Set<String> inputTypes) {
            this.query = query;
            this.inputTypes = inputTypes;
        }
    }

    public static boolean mayContainBBOMessages(TickStream stream) {
        return (mayContainSubclasses(stream, deltix.timebase.api.messages.BestBidOfferMessage.class));
    }

    public static boolean mayContainBarMessages(TickStream stream) {
        return (mayContainSubclasses(stream, deltix.timebase.api.messages.BarMessage.class));
    }

    public static boolean mayContainSubclasses(TickStream stream, Class<?> cls) {
        final String javaClassName = cls.getName();

        if (stream.isPolymorphic()) {
            for (RecordClassDescriptor rcd : stream.getPolymorphicDescriptors ())
                if (rcd.isConvertibleTo (javaClassName))
                    return (true);

            return (false);
        }
        else
            return (stream.getFixedType().isConvertibleTo(javaClassName));
    }

}
