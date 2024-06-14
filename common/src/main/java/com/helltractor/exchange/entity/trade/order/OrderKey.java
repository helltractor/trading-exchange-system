package com.helltractor.exchange.entity.trade.order;

import java.math.BigDecimal;

/**
 *ding
 *
 * @param sequenceId
 * @param price
 */
public record OrderKey(long sequenceId, BigDecimal price) {
}
