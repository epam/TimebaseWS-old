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

import com.epam.deltix.tbwg.webapp.services.charting.queries.LinesQuery;
import com.epam.deltix.timebase.api.rx.ReactiveMessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import io.reactivex.Observable;

import java.util.Set;

public class MessageSourceImpl implements MessageSource {

    private final LinesQuery query;
    private final ReactiveMessageSource source;
    private final Observable<InstrumentMessage> input;
    private final Set<String> types;

    public MessageSourceImpl(LinesQuery query, ReactiveMessageSource source, Observable<InstrumentMessage> input, Set<String> types) {
        this.query = query;
        this.source = source;
        this.input = input;
        this.types = types;
    }

    @Override
    public LinesQuery getQuery() {
        return query;
    }

    @Override
    public ReactiveMessageSource getSource() {
        return source;
    }

    @Override
    public Observable<InstrumentMessage> getInput() {
        return input;
    }

    @Override
    public Set<String> getTypes() {
        return types;
    }
}
