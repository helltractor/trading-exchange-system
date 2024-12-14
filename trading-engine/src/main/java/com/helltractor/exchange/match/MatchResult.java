package com.helltractor.exchange.match;

import com.helltractor.exchange.model.trade.OrderEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MatchResult {
    
    public final OrderEntity takerOrder;
    public final List<MatchDetailRecord> matchDetails = new ArrayList<>();
    
    public MatchResult(OrderEntity takerOrder) {
        this.takerOrder = takerOrder;
    }
    
    public void add(BigDecimal price, BigDecimal quantity, OrderEntity makerOrder) {
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
