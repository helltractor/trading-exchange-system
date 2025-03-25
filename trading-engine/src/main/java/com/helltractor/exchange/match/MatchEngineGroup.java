package com.helltractor.exchange.match;

import java.util.HashMap;
import java.util.Map;

public class MatchEngineGroup {
    
    final Map<Long, MatchEngine> engines = new HashMap<>();

//    public MatchResult processOrder(long sequenceId, OrderEntity order) {
//        Long symbolId = order.symbolId;
//        MatchEngine engine = engines.get(symbolId);
//        if (engine == null) {
//            engine = new MatchEngine();
//            engines.put(symbolId, engine);
//        }
//        return engine.processOrder(sequenceId, order);
//    }
}
