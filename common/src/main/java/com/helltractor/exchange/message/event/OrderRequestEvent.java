package com.helltractor.exchange.message.event;

import java.math.BigDecimal;

import com.helltractor.exchange.enums.Direction;

public class OrderRequestEvent extends AbstractEvent {

    public Long userId;

    public Direction direction;

    public BigDecimal price;

    public BigDecimal quantity;

    @Override
    public String toString() {
        return "OrderRequestEvent [sequenceId=" + sequenceId + ", previousId=" + previousId + ", uniqueId=" + uniqueId
                + ", refId=" + refId + ", createTime=" + createTime + ", userId=" + userId + ", direction=" + direction
                + ", price=" + price + ", quantity=" + quantity + "]";
    }
}
