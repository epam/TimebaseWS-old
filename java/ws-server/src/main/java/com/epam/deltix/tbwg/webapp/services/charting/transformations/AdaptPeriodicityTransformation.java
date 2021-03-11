package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.tbwg.messages.ChangePeriodicity;
import com.epam.deltix.tbwg.messages.Message;
import com.epam.deltix.timebase.api.messages.MarketMessageInfo;
import com.epam.deltix.timebase.api.messages.universal.BaseEntryInfo;
import com.epam.deltix.timebase.api.messages.universal.L2EntryNew;
import com.epam.deltix.timebase.api.messages.universal.PackageHeader;
import com.epam.deltix.timebase.api.messages.universal.PackageType;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import java.util.Collections;

public class AdaptPeriodicityTransformation extends ChartTransformation<MarketMessageInfo, MarketMessageInfo> {

    private final long maxPeriodicity;
    private final int maxLevels;
    private final PeriodicityFilter filter;

    public AdaptPeriodicityTransformation(int maxLevels , long periodicity) {
        super(Collections.singletonList(MarketMessageInfo.class), Collections.singletonList(MarketMessageInfo.class));

        this.maxLevels = maxLevels;
        this.maxPeriodicity = periodicity;
        this.filter = new PeriodicityFilter(periodicity, true);
    }

    @Override
    protected void onMessage(Message message) {
        sendMessage(message);
    }

    @Override
    protected void onNextPoint(MarketMessageInfo marketMessage) {
        if (marketMessage instanceof PackageHeader) {
            PackageHeader message = (PackageHeader) marketMessage;
            if (message.getPackageType() != PackageType.INCREMENTAL_UPDATE) {
                if (filter.test(message)) {
                    adaptPeriodicity(countLevels(message));
                }
            }
        }

        sendMessage(marketMessage);
    }

    private int countLevels(PackageHeader message) {
        if (message == null) {
            return 0;
        }

        ObjectArrayList<BaseEntryInfo> entries = message.getEntries();
        if (entries == null) {
            return 0;
        }

        int actualLevelsCount = -1;
        for (int i = 0; i < entries.size(); ++i) {
            BaseEntryInfo entry = entries.get(i);
            if (entry instanceof L2EntryNew) {
                short level = ((L2EntryNew) entry).getLevel();
                if (level > actualLevelsCount) {
                    actualLevelsCount = level;
                }
            }
        }

        return actualLevelsCount + 1;
    }

    private void adaptPeriodicity(int actualLevelsCount) {
        long newPeriodicity;
        if (actualLevelsCount < maxLevels && actualLevelsCount > 0) {
            double periodicityMultiplier = (double) actualLevelsCount / maxLevels;
            newPeriodicity = (long) ((double) maxPeriodicity * periodicityMultiplier);
        } else {
            newPeriodicity = maxPeriodicity;
        }

        if (filter.getPeriodicity() != newPeriodicity) {
            filter.setPeriodicity(newPeriodicity);
            sendMessage(new ChangePeriodicity(newPeriodicity));
        }
    }
}
