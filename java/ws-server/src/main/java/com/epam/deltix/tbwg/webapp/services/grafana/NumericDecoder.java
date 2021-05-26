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
package com.epam.deltix.tbwg.webapp.services.grafana;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.CodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.InterpretingCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.util.Map;

class NumericDecoder {

    private static final Log LOG = LogFactory.getLog(NumericDecoder.class);

    public CodecMetaFactory factory = InterpretingCodecMetaFactory.INSTANCE;
    private final ObjectToObjectHashMap<String, UnboundDecoder> decoders = new ObjectToObjectHashMap<String, UnboundDecoder>();
    private final MemoryDataInput input = new MemoryDataInput ();

    public void decode(RawMessage raw, Map<String, NumericListDelegate> current) {
        if (raw.data == null)
            return;

        final UnboundDecoder decoder = getDecoder (raw.type);
        input.setBytes (raw.data, raw.offset, raw.length);
        decoder.beginRead (input);
        while (decoder.nextField ()) {
            readField(decoder.getField(), decoder, current);
        }
    }

    private UnboundDecoder getDecoder(final RecordClassDescriptor type) {
        String guid = type.getGuid();
        UnboundDecoder decoder = decoders.get(guid, null);

        if (decoder == null) {
            decoder = factory.createFixedUnboundDecoderFactory(type).create();
            decoders.put(guid, decoder);
        }
        return decoder;
    }

    private void readField(NonStaticFieldInfo info, UnboundDecoder decoder, Map<String, NumericListDelegate> current) {
        try {
            if (info.getType() instanceof IntegerDataType) {
                readField(info.getName(), (IntegerDataType) info.getType(), decoder, current);
            } else if (info.getType() instanceof FloatDataType) {
                readField(info.getName(), (FloatDataType) info.getType(), decoder, current);
            } else if (info.getType() instanceof BooleanDataType) {
                boolean value = decoder.getBoolean();
                current.computeIfAbsent(info.getName(), key -> NumericLists.byteList()).addBoolean(value);
            } else {
                LOG.trace().append("Unsupported type ").append(info.getType()).commit();
            }
        } catch (NullValueException ignored) {
        }
    }

    private void readField(String name, IntegerDataType type, UnboundDecoder decoder, Map<String, NumericListDelegate> current) {
        int size = type.getNativeTypeSize();
        if (size >= 6) {
            current.computeIfAbsent(name, key -> NumericLists.longList()).addLong(decoder.getLong());
        } else if (size == 1) {
            current.computeIfAbsent(name, key -> NumericLists.byteList()).addByte((byte) decoder.getInt());
        } else if (size == 2) {
            current.computeIfAbsent(name, key -> NumericLists.shortList()).addShort((short) decoder.getInt());
        } else {
            current.computeIfAbsent(name, key -> NumericLists.intList()).addInt(decoder.getInt());
        }
    }

    private void readField(String name, FloatDataType type, UnboundDecoder decoder, Map<String, NumericListDelegate> current) {
        if (type.isFloat()) {
            float v = decoder.getFloat();
            if (Float.isFinite(v)) {
                current.computeIfAbsent(name, key -> NumericLists.floatList()).addFloat(v);
            }
        } else if (type.isDecimal64()) {
            @Decimal long v = decoder.getLong();
            if (Decimal64Utils.isFinite(v)) {
                current.computeIfAbsent(name, key -> NumericLists.doubleList()).addDecimal(v);
            }
        } else {
            double v = decoder.getDouble();
            if (Double.isFinite(v)) {
                current.computeIfAbsent(name, key -> NumericLists.doubleList()).addDouble(v);
            }
        }
    }
}
