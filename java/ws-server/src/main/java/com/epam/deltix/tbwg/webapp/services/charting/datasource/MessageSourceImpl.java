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
