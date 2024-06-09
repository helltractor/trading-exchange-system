package com.warp.exchange.order;

import java.math.BigDecimal;

/**
 *ding
 *
 * @param sequenceId
 * @param price
 */
public record OrderKey(long sequenceId, BigDecimal price) {
}
