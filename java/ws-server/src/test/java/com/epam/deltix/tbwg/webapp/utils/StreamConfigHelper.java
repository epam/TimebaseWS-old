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
package com.epam.deltix.tbwg.webapp.utils;

import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.api.messages.BarMessage;
import com.epam.deltix.timebase.api.messages.MarketMessage;

import java.util.ArrayList;
import java.util.List;

public class StreamConfigHelper {

    public static RecordClassDescriptor mkUniversalBarMessageDescriptor () {
        return (
            mkBarMessageDescriptor (
                null,
                null,
                null,
                FloatDataType.ENCODING_SCALE_AUTO,
                FloatDataType.ENCODING_SCALE_AUTO
            )
        );
    }

    public static RecordClassDescriptor     mkBarMessageDescriptor (
        RecordClassDescriptor   marketMsgDescriptor,
        String                  staticExchangeCode,
        Integer                 staticCurrencyCode,
        String                  priceEncoding,
        String                  sizeEncoding
    )
    {
        final String            name = BarMessage.class.getName ();

        final DataField[]      fields = {
            mkField (
                "exchangeId", "Exchange Code",
                new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false), null,
                staticExchangeCode
            ),
//            mkField (
//                "barSize", "Bar Size",
//                new IntegerDataType (IntegerDataType.ENCODING_PINTERVAL, false), null,
//                staticBarSize
//            ),
            new NonStaticDataField("close", "Close", new FloatDataType(priceEncoding, true)),
            new NonStaticDataField ("open", "Open", new FloatDataType (priceEncoding, true), "close"),
            new NonStaticDataField ("high", "High", new FloatDataType (priceEncoding, true), "close"),
            new NonStaticDataField ("low", "Low", new FloatDataType (priceEncoding, true), "close"),
            new NonStaticDataField ("volume", "Volume", new FloatDataType (sizeEncoding, true))
        };

        return (mkMarketMsgSubclassDescriptor (marketMsgDescriptor, name, staticCurrencyCode, fields));
    }

    public static DataField                 mkField (
        String                                  name,
        String                                  title,
        DataType                                type,
        String                                  relativeTo,
        Object                                  staticValue
    )
    {
        if (staticValue == null)
            return (new NonStaticDataField (name, title, type, false, relativeTo));
        else {
            String value = staticValue.toString();
            // special handling for ALPHANUMERIC: empty string means <null>
            if (type instanceof VarcharDataType && "ALPHANUMERIC(10)".equals(type.getEncoding())) {
                if (value.length() == 0)
                    value = null;
                else
                    ExchangeCodec.codeToLong((String) staticValue); // validate ALPHANUMERIC(10)
            }
            return (new StaticDataField(name, title, type, value));
        }
    }

    public static RecordClassDescriptor    mkMarketMsgSubclassDescriptor (
        RecordClassDescriptor   marketMsgDescriptor,
        String                  name,
        Integer                 staticCurrencyCode,
        DataField []            fields
    )
    {
        if (marketMsgDescriptor == null)
            marketMsgDescriptor =
                mkMarketMessageDescriptor (staticCurrencyCode);

        return new RecordClassDescriptor (
            name, name, false,
            marketMsgDescriptor,
            fields
        );
    }

    public static RecordClassDescriptor     mkMarketMessageDescriptor (
        Integer                 staticCurrencyCode
    )
    {
        return mkMarketMessageDescriptor(staticCurrencyCode, true);
    }

    public static RecordClassDescriptor     mkMarketMessageDescriptor (
        Integer                 staticCurrencyCode,
        boolean                 staticOriginalTimestamp
    )
    {
        return mkMarketMessageDescriptor(staticCurrencyCode, staticOriginalTimestamp, null);
    }

    public static RecordClassDescriptor     mkMarketMessageDescriptor (
        Integer                 staticCurrencyCode,
        Boolean                 staticOriginalTimestamp,
        Boolean                 staticNanoTime
    )
    {
        final String            name = MarketMessage.class.getName ();
        final List<DataField> fields = new ArrayList<>(4);
        if (staticOriginalTimestamp != null) {
            fields.add (
                staticOriginalTimestamp ?
                    new StaticDataField(
                        "originalTimestamp", "Original Time",
                        new DateTimeDataType(true), null) :
                    new NonStaticDataField(
                        "originalTimestamp", "Original Time",
                        new DateTimeDataType(true))

            );
        }


//nanoTime was removed
        if (staticNanoTime != null) {
            fields.add (
                staticNanoTime ?
                    new StaticDataField(
                        "nanoTime", "Nano Time",
                        new IntegerDataType(IntegerDataType.ENCODING_INT64, true), null) :
                    new NonStaticDataField(
                        "nanoTime", "Nano Time",
                        new IntegerDataType(IntegerDataType.ENCODING_INT64, true))
            );
        }


        fields.add (mkField (
            "currencyCode", "Currency Code",
            new IntegerDataType (IntegerDataType.ENCODING_INT16, true), null,
            staticCurrencyCode
        ));

        fields.add (new NonStaticDataField (
            "sequenceNumber", "Sequence Number",
            new IntegerDataType (IntegerDataType.ENCODING_INT64, true)
        ));

        return (new RecordClassDescriptor (name, name, true, null, fields.toArray(new DataField[fields.size()])));
    }

}
