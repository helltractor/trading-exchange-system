package com.warp.exchange.match;

import com.warp.exchange.order.Order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 匹配结果
 */
public class MatchResult {

    public final Order takerOrder;
    public final List<MatchDetailRecord> matchDetails = new ArrayList<>();

    public MatchResult(Order takerOrder) {
        this.takerOrder = takerOrder;
    }

    public void add(BigDecimal price, BigDecimal quantity, Order makerOrder) {
        matchDetails.add(new MatchDetailRecord(price, quantity, this.takerOrder, makerOrder));
    }

    @Override
    public String toString() {
        if (matchDetails.isEmpty()) {
            return "no matched.";
        }
        return matchDetails.size() + " matched:" + String.join(", ", matchDetails.stream().map(MatchDetailRecord::toString).toArray(String[]::new));
    }
}
