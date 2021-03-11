package com.epam.deltix.computations.data.text;

import com.epam.deltix.anvil.util.AsciiStringBuilder;
import com.epam.deltix.anvil.util.Reusable;
import com.epam.deltix.anvil.util.annotation.Alphanumeric;
import com.epam.deltix.anvil.util.codec.AlphanumericCodec;
import com.epam.deltix.computations.data.base.text.MutableAlphanumericValueInfo;

public class MutableAlphanumericValue implements MutableAlphanumericValueInfo, Reusable {

    @Alphanumeric
    private long value;

    private AsciiStringBuilder sb = null;

    public MutableAlphanumericValue(@Alphanumeric long value) {
        this.value = value;
    }

    public MutableAlphanumericValue() {
        this.value = ALPHANUMERIC_NULL;
    }

    @Override
    public void reuse() {
        value = LONG_NULL;
        if (sb != null) {
            sb.clear();
        }
    }

    @Override
    @Alphanumeric
    public long alphanumericValue() {
        return value;
    }

    @Override
    public void setAlphanumeric(@Alphanumeric long value) {
        this.value = value;
    }

    @Override
    public CharSequence charSequenceValue() {
        if (value == ALPHANUMERIC_NULL) {
            return null;
        }
        if (sb == null) {
            sb = new AsciiStringBuilder();
        }
        return AlphanumericCodec.decode(value, sb);
    }

    public static MutableAlphanumericValue of(CharSequence charSequence) {
        return new MutableAlphanumericValue(AlphanumericCodec.encode(charSequence));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + charSequenceValue();
    }
}
