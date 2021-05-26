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
package com.epam.deltix.tbwg.webapp.services.charting.transformations;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.quoteflow.orderbook.FullOrderBook;
import com.epam.deltix.quoteflow.orderbook.interfaces.OrderBookList;
import com.epam.deltix.quoteflow.orderbook.interfaces.OrderBookQuote;
import com.epam.deltix.tbwg.messages.ChangePeriodicity;
import com.epam.deltix.tbwg.messages.FeedStatusMessage;
import com.epam.deltix.tbwg.messages.Message;
import com.epam.deltix.tbwg.messages.OrderBookLinePoint;
import com.epam.deltix.timebase.api.messages.*;
import com.epam.deltix.timebase.api.messages.service.FeedStatus;
import com.epam.deltix.timebase.api.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The transformation builds quoteflow order book for instrument and sends snapshots with specified periodicity and max level.
 * The output messages are time series line points for each side and level of book.
 */
public class UniversalL2ToPointsTransformation extends ChartTransformation<OrderBookLinePoint, MarketMessageInfo> {

    private static final DataModelType DATA_MODEL = DataModelType.LEVEL_TWO;

    private final FullOrderBook book;
    private final int maxLevels;

    private final OrderBookLinePoint outputPoint = new OrderBookLinePoint();

    private final ArrayList<PackageHeader> messages = new ArrayList<>();
    private final ObjectArrayList<BaseEntryInfo> tempEntries = new ObjectArrayList<>();
    private final PeriodicityFilter filter;

    private boolean firstSnapshotSent;

    public UniversalL2ToPointsTransformation(String symbol, int maxLevels, long periodicity) {
        super(Collections.singletonList(MarketMessageInfo.class), Collections.singletonList(OrderBookLinePoint.class));

        this.book = new FullOrderBook(symbol, DATA_MODEL);
        this.maxLevels = maxLevels;

        this.filter = new PeriodicityFilter(periodicity, false);

        this.outputPoint.setSymbol(symbol);
    }

    @Override
    protected void onMessage(Message message) {
        if (message instanceof ChangePeriodicity) {
            filter.setPeriodicity(((ChangePeriodicity) message).getPeriodicity());
        } else if (message instanceof FeedStatusMessage) {
            FeedStatusMessage feedStatus = (FeedStatusMessage) message;
            if (feedStatus.getStatus() == FeedStatus.NOT_AVAILABLE) {
                filter.refresh();
                messages.clear();
                book.update(createPackageHeader(feedStatus.getExchangeId()));
                flushMessages(feedStatus.getTimestamp());
            }
        }

        sendMessage(message);
    }

    @Override
    protected void onNextPoint(MarketMessageInfo marketMessage) {
        if (marketMessage instanceof PackageHeader) {
            boolean isFirstSnapshot = false;
            PackageHeader message = (PackageHeader) marketMessage;
            if (message.getPackageType() != PackageType.INCREMENTAL_UPDATE) {
                // no sense to apply incremental updates before snapshot, skip them
                messages.clear();
                // periodical snapshots can be skipped by quote flow,
                // change type to vendor snapshot
                message.setPackageType(PackageType.VENDOR_SNAPSHOT);

                isFirstSnapshot = !firstSnapshotSent;
                firstSnapshotSent = true;
            }

            PackageHeader lastMessage = message.clone();
            if (filterEntries(lastMessage) > 0) {
                messages.add(lastMessage);
            }

            if (filter.test(lastMessage) || isFirstSnapshot) {
                flushMessages(lastMessage.getTimeStampMs());
            }
        }
    }

    private PackageHeaderInfo createPackageHeader(long exchangeId) {
        PackageHeader packageHeader = new PackageHeader();
        packageHeader.setPackageType(PackageType.VENDOR_SNAPSHOT);

        ObjectArrayList<BaseEntryInfo> entries = new ObjectArrayList<>();
        entries.add(createBookResetEntry(exchangeId, QuoteSide.BID));
        entries.add(createBookResetEntry(exchangeId, QuoteSide.ASK));
        packageHeader.setEntries(entries);

        return packageHeader;
    }

    private BookResetEntry createBookResetEntry(long exchangeId, QuoteSide side) {
        BookResetEntry resetEntry = new BookResetEntry();
        resetEntry.setSide(side);
        resetEntry.setModelType(DATA_MODEL);
        resetEntry.setExchangeId(exchangeId);

        return resetEntry;
    }

    private void flushMessages(long timestamp) {
        messages.forEach(book::update);
        messages.clear();

        sendQuotes(book.getAllAskQuotes(), QuoteSide.ASK, timestamp);
        sendQuotes(book.getAllBidQuotes(), QuoteSide.BID, timestamp);
    }

    private void sendQuotes(OrderBookList<OrderBookQuote> quotes, QuoteSide side, long timestamp) {
        int i = 0;
        for (; i < quotes.size(); ++i) {
            if (i >= maxLevels) {
                break;
            }

            OrderBookQuote quote = quotes.getObjectAt(i);
            sendQuote(timestamp, i, side, quote.getPrice(), quote.getSize());
        }

        for (; i < maxLevels; ++i) {
            sendQuote(timestamp, i, side, Decimal64Utils.NaN, Decimal64Utils.NaN);
        }
    }

    private void sendQuote(long timestamp, int level, QuoteSide side, long price, long size) {
        outputPoint.setTimeStampMs(timestamp);
        outputPoint.setLevel(level);
        outputPoint.setSide(side);
        outputPoint.setValue(price);
        outputPoint.setVolume(size);
        sendMessage(outputPoint);
    }

    private int filterEntries(PackageHeader msg) {
        ObjectArrayList<BaseEntryInfo> entries = msg.getEntries();

        tempEntries.clear();
        boolean changed = false;
        for (int i = 0; i < entries.size(); ++i) {
            BaseEntryInfo entry = entries.get(i);
            if (entry instanceof L2EntryUpdate) {
                L2EntryUpdate entryUpdate = (L2EntryUpdate) entry;
                if (entryUpdate.getAction() == BookUpdateAction.UPDATE) {
                    changed = true;
                    continue;
                }
            } else if (entry instanceof L3EntryUpdate) {
                L3EntryUpdate entryUpdate = (L3EntryUpdate) entry;
                if (entryUpdate.getAction() == QuoteUpdateAction.MODIFY) {
                    changed = true;
                    continue;
                }
            }

            tempEntries.add(entry);
        }

        if (changed) {
            if (tempEntries.size() == 0) {
                return 0;
            }

            entries.clear();
            entries.addAll(tempEntries);
        }

        return entries.size();
    }
}
