package com.helltractor.exchange.bean;

import com.helltractor.exchange.enums.MatchType;

import java.math.BigDecimal;

public record SimpleMatchDetailRecord(BigDecimal price, BigDecimal quantity, MatchType type) {
}
