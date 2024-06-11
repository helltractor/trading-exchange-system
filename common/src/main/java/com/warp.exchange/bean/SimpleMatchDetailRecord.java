package com.warp.exchange.bean;

import com.warp.exchange.enums.MatchType;

import java.math.BigDecimal;

public record SimpleMatchDetailRecord(BigDecimal price, BigDecimal quantity, MatchType type) {
}
