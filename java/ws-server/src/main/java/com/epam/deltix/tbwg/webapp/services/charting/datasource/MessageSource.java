package com.epam.deltix.tbwg.webapp.services.charting.datasource;

import com.epam.deltix.tbwg.webapp.services.charting.queries.LinesQuery;
import com.epam.deltix.timebase.api.rx.ReactiveMessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import io.reactivex.Observable;

import java.util.Set;

public interface MessageSource {

    LinesQuery              getQuery();

    ReactiveMessageSource   getSource();

    Observable<InstrumentMessage> getInput();

    Set<String>             getTypes();
}
