package com.warp.exchange.match;

import com.warp.exchange.order.Order;

import java.math.BigDecimal;
import java.util.Objects;

public class MatchDetailRecord {

    BigDecimal price;
    public BigDecimal quantity;
    Order takerOrder;
    public Order makerOrder;

    public MatchDetailRecord(BigDecimal price, BigDecimal quantity, Order takerOrder, Order makerOrder) {
        this.price = price;
        this.quantity = quantity;
        this.takerOrder = takerOrder;
        this.makerOrder = makerOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchDetailRecord that = (MatchDetailRecord) o;
        return Objects.equals(price, that.price) &&
                Objects.equals(quantity, that.quantity) &&
                Objects.equals(takerOrder, that.takerOrder) &&
                Objects.equals(makerOrder, that.makerOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, quantity, takerOrder, makerOrder);
    }
}
