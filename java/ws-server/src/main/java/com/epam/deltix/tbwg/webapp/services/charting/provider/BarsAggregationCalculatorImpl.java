package com.epam.deltix.tbwg.webapp.services.charting.provider;

import com.epam.deltix.tbwg.webapp.services.charting.TimeInterval;

public class BarsAggregationCalculatorImpl implements AggregationCalculator {

    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = 60 * ONE_SECOND;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    private static final long ONE_DAY = 24 * ONE_HOUR;
    private static final long ONE_WEEK = 7 * ONE_DAY;
    private static final long TWO_WEEKS = 2 * ONE_WEEK;
    private static final long ONE_MONTH = 30 * ONE_DAY;
    private static final long ONE_YEAR = 12 * ONE_MONTH;

    @Override
    public long getAggregation(TimeInterval interval) {
        long windowSizeMs = interval.getEndTimeMilli() - interval.getStartTimeMilli();
        if (windowSizeMs < ONE_MINUTE) {
            return 0;
        } else if (windowSizeMs < 10 * ONE_MINUTE) {
            return ONE_SECOND;
        } else if (windowSizeMs < ONE_HOUR) {
            return 10 * ONE_SECOND;
        } else if (windowSizeMs < ONE_DAY) {
            return ONE_MINUTE;
        } else if (windowSizeMs < ONE_WEEK) {
            return 5 * ONE_MINUTE;
        } else if (windowSizeMs < TWO_WEEKS) {
            return 10 * ONE_MINUTE;
        } else if (windowSizeMs < ONE_MONTH) {
            return 30 * ONE_MINUTE;
        } else if (windowSizeMs < ONE_YEAR) {
            return ONE_HOUR;
        }

        return ONE_DAY;
    }

    @Override
    public long getNewWindowSize(TimeInterval interval) {
        long windowSizeMs = interval.getEndTimeMilli() - interval.getStartTimeMilli();

        if (windowSizeMs < ONE_MINUTE) {
            return 0;
        } else if (windowSizeMs < 10 * ONE_MINUTE) {
            return ONE_MINUTE;
        } else if (windowSizeMs < ONE_HOUR) {
            return 10 * ONE_MINUTE;
        } else if (windowSizeMs < ONE_DAY) {
            return ONE_HOUR;
        } else if (windowSizeMs < ONE_WEEK) {
            return ONE_DAY;
        } else if (windowSizeMs < TWO_WEEKS) {
            return ONE_WEEK;
        } else if (windowSizeMs < ONE_MONTH) {
            return TWO_WEEKS;
        } else if (windowSizeMs < ONE_YEAR) {
            return ONE_MONTH;
        }

        return ONE_YEAR;
    }


}
