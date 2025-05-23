package com.helltractor.exchange.match;

import java.math.BigDecimal;

import com.helltractor.exchange.model.trade.OrderEntity;

public record MatchDetailRecord(BigDecimal price, BigDecimal quantity, OrderEntity takerOrder, OrderEntity makerOrder) {

}
