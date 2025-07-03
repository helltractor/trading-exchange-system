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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class TradingEngineServiceBenchmark {
    
    static final Long USER_A = 11111L;
    static final Long USER_B = 22222L;
    static final Long USER_C = 33333L;
    static final Long USER_D = 44444L;
    static final Long[] USERS = {USER_A, USER_B, USER_C, USER_D};
    
    long currentSequenceId;
    TradingEngineService engine;
    Random random;
    
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(TradingEngineServiceBenchmark.class.getSimpleName())
                .warmupIterations(1)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(1)
                .measurementTime(TimeValue.seconds(1))
                .forks(1)
                .build();
        
        new Runner(options).run();
    }
    
    @Setup(Level.Invocation)
    public void setup() {
        this.engine = createTradingEngine();
        this.random = new Random(123456789);
        this.currentSequenceId = 0;
        
        for (Long user : USERS) {
            engine.processEvent(depositEvent(user, random(this.random, 1000_0000, 2000_0000), AssetEnum.USD));
            engine.processEvent(depositEvent(user, random(this.random, 1000, 2000), AssetEnum.BTC));
        }
        
    }
    
    @TearDown
    public void tearDown() {
        this.engine = null;
        this.random = null;
        this.currentSequenceId = 0;
    }
    
    @Benchmark
    public void callOrders() {
        for (int i = 0; i < 100_000; i++) {
            Long user = USERS[i % USERS.length];
            engine.processEvent(orderRequestEvent(user, Direction.BUY, bd("30000.00"), bd("0.001")));
        }
        for (int i = 0; i < 100_000; i++) {
            Long user = USERS[i % USERS.length];
            engine.processEvent(orderRequestEvent(user, Direction.SELL, bd("30000.00"), bd("0.001")));
        }
        
        engine.validate();
    }
    
    @Benchmark
    public void callCancelOrders() {
        for (int i = 0; i < 100_000; i++) {
            Long user = USERS[i % USERS.length];
            engine.processEvent(orderRequestEvent(user, Direction.BUY, bd("30000.00"), bd("0.001")));
            engine.processEvent(orderCancelEvent(user, 1000L + i % 1000));
            engine.processEvent(orderRequestEvent(user, Direction.SELL, bd("30000.00"), bd("0.001")));
            engine.processEvent(orderCancelEvent(user, 1000L + i % 1000));
        }
        
        engine.validate();
    }
    
    @Benchmark
    public void callRandomOrders() {
        int low = 20000;
        int high = 40000;
        for (int i = 0; i < 1000; i++) {
            Long user = USERS[i % USERS.length];
            engine.processEvent(orderRequestEvent(user, Direction.BUY, random(this.random, low, high), random(this.random, 1, 5)));
            engine.processEvent(orderRequestEvent(user, Direction.SELL, random(this.random, low, high), random(this.random, 1, 5)));
        }
        
        engine.validate();
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
        int n = random.nextInt(high - low) + low;
        int m = random.nextInt(100);
        return new BigDecimal(n + "." + m);
    }
} 
