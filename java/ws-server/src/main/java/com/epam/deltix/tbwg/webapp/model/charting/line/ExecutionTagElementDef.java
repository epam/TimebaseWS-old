package com.epam.deltix.tbwg.webapp.model.charting.line;

import com.epam.deltix.timebase.api.messages.QuoteSide;

import java.util.Objects;

/**
 * A line point that defines an execution tag.
 * @label ExecutionTag
 */
public class ExecutionTagElementDef extends TagElementDef {
    private String price;
    private QuoteSide side;
    private String size;

    /**
     * Execution price.
     */
    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    /**
     * Execution side.
     */
    public QuoteSide getSide() {
        return side;
    }

    public void setSide(QuoteSide side) {
        this.side = side;
    }

    /**
     * Execution size.
     */
    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExecutionTagElementDef that = (ExecutionTagElementDef) o;
        return Objects.equals(price, that.price) &&
            side == that.side &&
            Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), price, side, size);
    }
}
