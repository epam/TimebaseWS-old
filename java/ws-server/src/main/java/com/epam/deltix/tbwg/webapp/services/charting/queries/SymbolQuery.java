package com.epam.deltix.tbwg.webapp.services.charting.queries;

public interface SymbolQuery extends LinesQuery {

    String          getStream();

    String          getSymbol();

}
