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
package com.epam.deltix.computations.data.text;

import com.epam.deltix.anvil.util.AsciiStringBuilder;
import com.epam.deltix.anvil.util.annotation.Alphanumeric;
import com.epam.deltix.anvil.util.codec.AlphanumericCodec;
import com.epam.deltix.computations.data.base.text.MutableCharSequenceValueInfo;

public class MutableCharSequenceValue implements MutableCharSequenceValueInfo {

    private final StringBuilder sb = new StringBuilder();
    private final AsciiStringBuilder ascii = new AsciiStringBuilder();

    private CharSequence value;

    @Override
    public void reuse() {
        sb.setLength(0);
        value = null;
    }

    @Override
    public String value() {
        return value.toString();
    }

    @Override
    public CharSequence charSequenceValue() {
        return value;
    }

    @Override
    public void set(CharSequence value) {
        if (value == null) {
            this.value = null;
        } else {
            sb.setLength(0);
            sb.append(value);
            this.value = sb;
        }
    }

    @Override
    public void setAlphanumeric(@Alphanumeric long value) {
        if (value == ALPHANUMERIC_NULL) {
            this.value = null;
        } else {
            sb.setLength(0);
            AlphanumericCodec.decode(value, ascii);
            sb.append(ascii);
            this.value = sb;
        }
    }

    public static MutableCharSequenceValue of(CharSequence charSequence) {
        MutableCharSequenceValue charSequenceValue = new MutableCharSequenceValue();
        charSequenceValue.set(charSequence);
        return charSequenceValue;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + value();
    }
}
