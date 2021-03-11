package com.epam.deltix.tbwg.messages;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.timebase.api.messages.QuoteSide;
import com.epam.deltix.timebase.messages.*;

@SchemaElement(
    name = "deltix.tbwg.messages.OrderBookLinePoint",
    title = "Level Line Point"
)
public class OrderBookLinePoint extends LinePoint {

    public static final String CLASS_NAME = OrderBookLinePoint.class.getName();

    protected int level = TypeConstants.INT32_NULL;

    protected QuoteSide side;

    @Decimal
    protected long volume = TypeConstants.DECIMAL_NULL;

    @SchemaElement(
        title = "Algo Id"
    )
    @SchemaType(
        isNullable = false
    )
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @SchemaType(
        isNullable = false
    )
    @SchemaElement(
        title = "Side"
    )
    public QuoteSide getSide() {
        return side;
    }

    public void setSide(QuoteSide side) {
        this.side = side;
    }

    @Decimal
    @SchemaElement(
        title = "Volume"
    )
    @SchemaType(
        encoding = "DECIMAL64",
        dataType = SchemaDataType.FLOAT
    )
    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    @Override
    protected OrderBookLinePoint createInstance() {
        return new OrderBookLinePoint();
    }

    @Override
    public OrderBookLinePoint clone() {
        OrderBookLinePoint t = createInstance();
        t.copyFrom(this);
        return t;
    }

    @Override
    public InstrumentMessage copyFrom(RecordInfo source) {
        super.copyFrom(source);
        if (source instanceof OrderBookLinePoint) {
            final OrderBookLinePoint obj = (OrderBookLinePoint) source;
            level = obj.level;
            side = obj.side;
            volume = obj.volume;
        }
        return this;
    }
}
