package com.epam.deltix.tbwg.webapp.model.filter;

import com.epam.deltix.tbwg.webapp.utils.qql.SelectBuilder;

import java.util.List;

/**
 * @author Daniil Yarmalkevich
 * Date: 6/24/2019
 */
public class EqualFilter extends Filter {

    public EqualFilter(String field, List<?> data) {
        super(field, data);
    }

    @Override
    public SelectBuilder appendTo(SelectBuilder selectBuilder) throws SelectBuilder.NoSuchFieldException,
            SelectBuilder.WrongTypeException {
        return selectBuilder.field(field).equalTo(getList());
    }
}
