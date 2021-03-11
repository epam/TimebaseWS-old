package com.epam.deltix.tbwg.webapp.model.filter;

import com.epam.deltix.tbwg.webapp.utils.qql.SelectBuilder;

/**
 * @author Daniil Yarmalkevich
 * Date: 6/24/2019
 */
public class NotNullFilter extends Filter {
    public NotNullFilter(String field) {
        super(field, null);
    }

    @Override
    public SelectBuilder appendTo(SelectBuilder selectBuilder) throws SelectBuilder.NoSuchFieldException,
            SelectBuilder.WrongTypeException {
        return selectBuilder.field(field).notNull();
    }
}
