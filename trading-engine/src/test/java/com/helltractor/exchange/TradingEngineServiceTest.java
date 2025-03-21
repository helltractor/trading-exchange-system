package com.helltractor.exchange;

import com.helltractor.exchange.assets.AssetService;
import com.helltractor.exchange.clearing.ClearingService;
import com.helltractor.exchange.enums.AssetEnum;
import com.helltractor.exchange.enums.Direction;
import com.helltractor.exchange.enums.UserType;
import com.helltractor.exchange.match.MatchEngine;
import com.helltractor.exchange.message.event.AbstractEvent;
import com.helltractor.exchange.message.event.OrderCancelEvent;
import com.helltractor.exchange.message.event.OrderRequestEvent;
import com.helltractor.exchange.message.event.TransferEvent;
import com.helltractor.exchange.order.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

public class TradingEngineServiceTest {
    
    static final Long USER_A = 11111L;
    static final Long USER_B = 22222L;
    static final Long USER_C = 33333L;
    static final Long USER_D = 44444L;
    static final Long[] USERS = {USER_A, USER_B, USER_C, USER_D};
    long currentSequenceId = 0;
    
    @Test
    void testTradingEngine() {
        var engine = createTradingEngine();
        
        engine.processEvent(depositEvent(USER_A, bd("58000"), AssetEnum.USD));
        engine.processEvent(depositEvent(USER_B, bd("126700"), AssetEnum.USD));
        engine.processEvent(depositEvent(USER_C, bd("5.5"), AssetEnum.BTC));
        engine.processEvent(depositEvent(USER_D, bd("8.6"), AssetEnum.BTC));
        
        engine.debug();
        engine.validate();
        
        engine.processEvent(orderRequestEvent(USER_A, Direction.BUY, bd("2207.33"), bd("1.2")));
        engine.processEvent(orderRequestEvent(USER_C, Direction.SELL, bd("2215.6"), bd("0.8")));
        engine.processEvent(orderRequestEvent(USER_C, Direction.SELL, bd("2921.1"), bd("0.3")));
        
        engine.debug();
        engine.validate();
        
        engine.processEvent(orderRequestEvent(USER_D, Direction.SELL, bd("2206"), bd("0.3")));
        
        engine.debug();
        engine.validate();
        
        engine.processEvent(orderRequestEvent(USER_B, Direction.BUY, bd("2219.6"), bd("2.4")));
        
        engine.debug();
        engine.validate();
        
        engine.processEvent(orderCancelEvent(USER_A, 1L));
        
        engine.debug();
        engine.validate();
    }
    
    @Test
    void testRandom() {
        var engine = createTradingEngine();
        var r = new Random(123456789);
        for (Long user : USERS) {
            engine.processEvent(depositEvent(user, random(r, 1000_0000, 2000_0000), AssetEnum.USD));
            engine.processEvent(depositEvent(user, random(r, 1000, 2000), AssetEnum.BTC));
        }
        engine.debug();
        engine.validate();
        
        int low = 20000;
        int high = 40000;
        for (int i = 0; i < 100; i++) {
            Long user = USERS[i % USERS.length];
            engine.processEvent(orderRequestEvent(user, Direction.BUY, random(r, low, high), random(r, 1, 5)));
            engine.debug();
            engine.validate();
            
            engine.processEvent(orderRequestEvent(user, Direction.SELL, random(r, low, high), random(r, 1, 5)));
            engine.debug();
            engine.validate();
        }
        
        Assertions.assertEquals("35216.4", engine.matchEngine.marketPrice.stripTrailingZeros().toPlainString());
    }
    
    TradingEngineService createTradingEngine() {
        var tradingEngine = new TradingEngineService();
        var matchEngine = new MatchEngine();
        var assetService = new AssetService();
        var orderService = new OrderService(assetService);
        var clearingService = new ClearingService(assetService, orderService);
        tradingEngine.matchEngine = matchEngine;
        tradingEngine.assetService = assetService;
        tradingEngine.orderService = orderService;
        tradingEngine.clearingService = clearingService;
        return tradingEngine;
    }
    
    <T extends AbstractEvent> T createEvent(Class<T> clazz) {
        T event;
        try {
            event = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        event.previousId = this.currentSequenceId;
        this.currentSequenceId++;
        event.sequenceId = this.currentSequenceId;
        event.createTime = LocalDateTime.parse("2022-02-22T22:22:22").atZone(ZoneId.of("Z")).toEpochSecond() * 1000
                + this.currentSequenceId;
        return event;
    }
    
    OrderRequestEvent orderRequestEvent(long userId, Direction direction, BigDecimal price, BigDecimal quantity) {
        var event = createEvent(OrderRequestEvent.class);
        event.userId = userId;
        event.direction = direction;
        event.price = price;
        event.quantity = quantity;
        return event;
    }
    
    OrderCancelEvent orderCancelEvent(long userId, Long orderId) {
        var event = createEvent(OrderCancelEvent.class);
        event.userId = userId;
        event.refOrderId = orderId;
        return event;
    }
    
    TransferEvent depositEvent(long userId, BigDecimal amount, AssetEnum asset) {
        var event = createEvent(TransferEvent.class);
        event.fromUserId = UserType.DEBT.getInternalUserId();
        event.toUserId = userId;
        event.amount = amount;
        event.asset = asset;
        event.sufficient = false;
        return event;
    }
    
    BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
    
    BigDecimal random(Random random, int low, int high) {
        int n = random.nextInt(low, high);
        int m = random.nextInt(100);
        return new BigDecimal(n + "." + m);
    }
}
