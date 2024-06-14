package com.warp.exchange.match;

import com.warp.exchange.entity.trade.OrderEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单撮合引擎组
 */
public class MatchEngineGroup {
    final Map<Long, MatchEngine> engines = new HashMap<>();
    
    public MatchResult processOrder(long sequenceId, OrderEntity order) {
        Long symbolId = order.symbolId;
        MatchEngine engine = engines.get(symbolId);
        if (engine == null) {
            engine = new MatchEngine();
            engines.put(symbolId, engine);
        }
        return engine.processOrder(sequenceId, order);
    }
}
