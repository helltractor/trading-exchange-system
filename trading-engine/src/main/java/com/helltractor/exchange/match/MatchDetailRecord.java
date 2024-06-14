package com.helltractor.exchange.match;

import com.helltractor.exchange.entity.trade.order.OrderEntity;

import java.math.BigDecimal;

public record MatchDetailRecord(BigDecimal price, BigDecimal quantity, OrderEntity takerOrder, OrderEntity makerOrder) {
}
