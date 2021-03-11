package com.epam.deltix.tbwg.webapp.model.charting.line;

import java.util.Objects;

public class BarElementDef extends LineElementDef {

    private String open;
    private String close;
    private String low;
    private String high;
    private String volume;

    public BarElementDef() {
    }

    public String getOpen() {
        return open;
    }

    public void setOpen(String open) {
        this.open = open;
    }

    public String getClose() {
        return close;
    }

    public void setClose(String close) {
        this.close = close;
    }

    public String getLow() {
        return low;
    }

    public void setLow(String low) {
        this.low = low;
    }

    public String getHigh() {
        return high;
    }

    public void setHigh(String high) {
        this.high = high;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BarElementDef that = (BarElementDef) o;
        return Objects.equals(open, that.open) &&
            Objects.equals(close, that.close) &&
            Objects.equals(low, that.low) &&
            Objects.equals(high, that.high) &&
            Objects.equals(volume, that.volume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), open, close, low, high, volume);
    }
}
