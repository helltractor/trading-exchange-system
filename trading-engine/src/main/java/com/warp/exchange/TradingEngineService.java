package com.warp.exchange;

import com.warp.exchange.bean.OrderBookBean;
import com.warp.exchange.entity.quatation.TickEntity;
import com.warp.exchange.entity.trade.MatchDetailEntity;
import com.warp.exchange.entity.trade.OrderEntity;
import com.warp.exchange.enums.*;
import com.warp.exchange.match.MatchDetailRecord;
import com.warp.exchange.match.MatchEngine;
import com.warp.exchange.match.MatchResult;
import com.warp.exchange.message.ApiResultMessage;
import com.warp.exchange.message.NotificationMessage;
import com.warp.exchange.message.TickMessage;
import com.warp.exchange.message.event.AbstractEvent;
import com.warp.exchange.message.event.OrderCancelEvent;
import com.warp.exchange.message.event.OrderRequestEvent;
import com.warp.exchange.message.event.TransferEvent;
import com.warp.exchange.messaging.MessageConsumer;
import com.warp.exchange.messaging.MessageProducer;
import com.warp.exchange.messaging.Messaging;
import com.warp.exchange.messaging.MessagingFactory;
import com.warp.exchange.redis.RedisCache;
import com.warp.exchange.redis.RedisService;
import com.warp.exchange.service.AssetService;
import com.warp.exchange.service.ClearingService;
import com.warp.exchange.service.OrderService;
import com.warp.exchange.service.StoreService;
import com.warp.exchange.support.LoggerSupport;
import com.warp.exchange.trade.asset.Asset;
import com.warp.exchange.util.IpUtil;
import com.warp.exchange.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

@Component
public class TradingEngineService extends LoggerSupport {
    
    @Autowired(required = false)
    ZoneId zoneId = ZoneId.systemDefault();
    
    @Value("#{exchangeConfiguration.orderBookDepth}")
    int orderBookDepth = 100;
    
    @Value("#{exchangeConfiguration.debugMode}")
    boolean debugMode = false;
    
    boolean fatalError = false;
    
    @Autowired
    ClearingService clearingService;
    
    @Autowired
    OrderService orderService;
    
    @Autowired
    AssetService assetService;
    
    @Autowired
    MatchEngine matchEngine;
    
    @Autowired
    RedisService redisService;
    
    @Autowired
    StoreService storeService;
    
    @Autowired
    MessagingFactory messagingFactory;
    
    MessageConsumer consumer;
    
    MessageProducer<TickMessage> producer;
    
    long lastSequenceId = 0;
    
    boolean orderBookChanged = false;
    
    String shaUpdateOrderBookLua;
    
    Thread tickThread;
    Thread notifyThread;
    Thread apiResultThread;
    Thread orderBookThread;
    Thread dbThread;
    
    OrderBookBean lastedOrderBook = null;
    Queue<List<OrderEntity>> orderQueue = new ConcurrentLinkedQueue<>();
    Queue<List<MatchDetailEntity>> matchQueue = new ConcurrentLinkedQueue<>();
    Queue<TickMessage> tickQueue = new ConcurrentLinkedQueue<>();
    Queue<ApiResultMessage> apiResultQueue = new ConcurrentLinkedQueue<>();
    Queue<NotificationMessage> notificationQueue = new ConcurrentLinkedQueue<>();
    
    @PostConstruct
    public void init() {
        this.shaUpdateOrderBookLua = this.redisService.loadScriptFromClassPath("/redis/update-orderbook.lua");
        this.consumer = this.messagingFactory.createBatchMessageListener(Messaging.Topic.TRADE, IpUtil.getHostId(),
                this::processMessages);
        this.producer = this.messagingFactory.createMessageProducer(Messaging.Topic.TICK, TickMessage.class);
        this.tickThread = new Thread(this::runTickThread, "async-tick");
        this.tickThread.start();
        this.notifyThread = new Thread(this::runNotifyThread, "async-notify");
        this.notifyThread.start();
        this.orderBookThread = new Thread(this::runOrderBookThread, "async-orderbook");
        this.orderBookThread.start();
        this.apiResultThread = new Thread(this::runApiResultThread, "async-api-result");
        this.apiResultThread.start();
        this.dbThread = new Thread(this::runDbThread, "async-db");
        this.dbThread.start();
    }
    
    @PreDestroy
    public void destroy() {
        this.consumer.stop();
        this.orderBookThread.interrupt();
        this.dbThread.interrupt();
    }
    
    void runTickThread() {
        logger.info("start tick thread...");
        for (; ; ) {
            List<TickMessage> messages = new ArrayList<>();
            for (; ; ) {
                TickMessage message = this.tickQueue.poll();
                if (message != null) {
                    messages.add(message);
                    if (messages.size() >= 1000) {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (!messages.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("send {} tick messages...", messages.size());
                }
                this.producer.sendMessages(messages);
            } else {
                // 无TickMessage时，暂停1ms:
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }
    
    void runNotifyThread() {
        logger.info("start notify thread...");
        for (; ; ) {
            NotificationMessage message = this.notificationQueue.poll();
            if (message != null) {
                redisService.publish(RedisCache.Topic.NOTIFICATION, JsonUtil.writeJson(message));
            } else {
                // 无NotificationMessage时，暂停1ms:
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }
    
    void runOrderBookThread() {
        logger.info("start update orderbook snapshot to redis...");
        long lastSequenceId = 0;
        for (; ; ) {
            // 获取OrderBookBean的引用，确保后续操作针对局部变量而非成员变量
            final OrderBookBean orderBook = this.lastedOrderBook;
            // 仅在OrderBookBean更新后刷新Redis
            if (orderBook != null && orderBook.sequenceId > lastSequenceId) {
                if (logger.isDebugEnabled()) {
                    logger.debug("update orderbook snapshot at sequence id {}...", orderBook.sequenceId);
                }
                redisService.executeScriptReturnBoolean(this.shaUpdateOrderBookLua,
                        // keys: [cache-key]
                        new String[]{RedisCache.Key.ORDER_BOOK},
                        // args: [sequenceId, json-data]
                        new String[]{String.valueOf(orderBook.sequenceId), JsonUtil.writeJson(orderBook)});
                lastSequenceId = orderBook.sequenceId;
            } else {
                // 无更新时，暂停1ms
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }
    
    void runApiResultThread() {
        logger.info("start publish api result to redis...");
        for (; ; ) {
            ApiResultMessage result = this.apiResultQueue.poll();
            if (result != null) {
                redisService.publish(RedisCache.Topic.TRADING_API_RESULT, JsonUtil.writeJson(result));
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }
    
    void runDbThread() {
        logger.info("start batch insert to database...");
        for (; ; ) {
            try {
                saveToDb();
            } catch (InterruptedException e) {
                logger.warn("{} was interrupted.", Thread.currentThread().getName());
                break;
            }
        }
    }
    
    void processMessages(List<AbstractEvent> messages) {
        for (AbstractEvent message : messages) {
            processEvent(message);
        }
        if (this.orderBookChanged) {
            // 获取最新的OrderBook快照
            this.lastedOrderBook = this.matchEngine.getOrderBook(this.orderBookDepth);
        }
    }
    
    void processEvent(AbstractEvent event) {
        if (fatalError) {
            return;
        }
        // 判断是否重复消息
        if (event.sequenceId <= this.lastSequenceId) {
            logger.warn("duplicated event: {}", event);
            return;
        }
        // 判断是否丢失了消息
        if (event.previousId > this.lastSequenceId) {
            List<AbstractEvent> events = storeService.loadEventFromDb(this.lastSequenceId);
            if (events.isEmpty()) {
                logger.error("cannot load lost event from db.");
                panic();
                return;
            }
            for (AbstractEvent e : events) {
                this.processEvent(e);
            }
            return;
        }
        // 判断当前消息是否指向上一条消息
        if (event.previousId != this.lastSequenceId) {
            logger.error("bad event: expected previous id {} but actual {} for event: {}", this.lastSequenceId,
                    event.previousId, event);
            panic();
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("process event {} -> {}: {}...", this.lastSequenceId, event.sequenceId, event);
        }
        try {
            if (event instanceof OrderRequestEvent) {
                createOrder((OrderRequestEvent) event);
            } else if (event instanceof OrderCancelEvent) {
                cancelOrder((OrderCancelEvent) event);
            } else if (event instanceof TransferEvent) {
                transfer((TransferEvent) event);
            } else {
                logger.error("unable to process event type: {}", event.getClass().getName());
                panic();
                return;
            }
        } catch (Exception e) {
            logger.error("process event error.", e);
            panic();
            return;
        }
        this.lastSequenceId = event.sequenceId;
        if (logger.isDebugEnabled()) {
            logger.debug("set last processed sequence id: {}...", this.lastSequenceId);
        }
        if (debugMode) {
            this.validate();
        }
    }
    
    void createOrder(OrderRequestEvent event) {
        // 创建订单ID
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.createTime), zoneId);
        int year = zonedDateTime.getYear();
        int month = zonedDateTime.getMonthValue();
        long orderId = event.sequenceId * 10000 + (year * 100 + month);
        // 创建订单
        OrderEntity order = orderService.createOrder(event.sequenceId, event.createTime, orderId, event.userId, event.direction, event.price, event.quantity);
        if (order == null) {
            logger.warn("create order failed.");
            // 推送失败结果
            this.apiResultQueue.add(ApiResultMessage.createOrderFailed(event.refId, event.createTime));
            return;
        }
        // 撮合和清算
        MatchResult result = this.matchEngine.processOrder(event.sequenceId, order);
        this.clearingService.clearMatchResult(result);
        // 推送成功结果,注意必须复制一份OrderEntity,因为将异步序列化
        this.apiResultQueue.add(ApiResultMessage.orderSuccess(event.refId, order.copy(), event.createTime));
        this.orderBookChanged = true;
        // 收集Notification
        List<NotificationMessage> notifications = new ArrayList<>();
        notifications.add(createNotification(event.createTime, "order_matched", order.userId, order.copy()));
        // TODO: 收集已完成的OrderEntity并生成MatchDetailEntity, TickEntity
        if (!result.matchDetails.isEmpty()) {
            List<OrderEntity> closedOrders = new ArrayList<>();
            List<MatchDetailEntity> matchDetails = new ArrayList<>();
            List<TickEntity> ticks = new ArrayList<>();
            if (result.takerOrder.status.isFinalStatus) {
                closedOrders.add(result.takerOrder);
            }
            for (MatchDetailRecord detail : result.matchDetails) {
                OrderEntity makerOrder = detail.makerOrder();
                if (makerOrder.status.isFinalStatus) {
                    closedOrders.add(makerOrder);
                }
                MatchDetailEntity takerDetail = generateMatchDetailEntity(event.sequenceId, event.createTime, detail,
                        true);
                MatchDetailEntity makerDetail = generateMatchDetailEntity(event.sequenceId, event.createTime, detail,
                        false);
                matchDetails.add(takerDetail);
                matchDetails.add(makerDetail);
                TickEntity tick = new TickEntity();
                tick.sequenceId = event.sequenceId;
                tick.takerOrderId = detail.takerOrder().id;
                tick.makerOrderId = detail.makerOrder().id;
                tick.price = detail.price();
                tick.quantity = detail.quantity();
                tick.takerDirection = detail.takerOrder().direction == Direction.BUY;
                tick.createTime = event.createTime;
                ticks.add(tick);
            }
            this.orderQueue.add(closedOrders);
            // 异步写入数据库
            this.orderQueue.add(closedOrders);
            this.matchQueue.add(matchDetails);
            // 异步发送Tick消息
            TickMessage msg = new TickMessage();
            msg.sequenceId = event.sequenceId;
            msg.createTime = event.createTime;
            msg.ticks = ticks;
            this.tickQueue.add(msg);
            // 异步通知OrderMatch
            this.notificationQueue.addAll(notifications);
        }
    }
    
    NotificationMessage createNotification(long ts, String type, Long userId, Object data) {
        NotificationMessage msg = new NotificationMessage();
        msg.createTime = ts;
        msg.type = type;
        msg.userId = userId;
        msg.data = data;
        return msg;
    }
    
    void cancelOrder(OrderCancelEvent event) {
        OrderEntity order = this.orderService.getOrder(event.refOrderId);
        
        if (order == null || order.userId.longValue() != event.userId.longValue()) {
            // 发送失败消息
            this.apiResultQueue.add(ApiResultMessage.cancelOrderFailed(event.refId, event.createTime));
            return;
        }
        this.matchEngine.cancel(event.createTime, order);
        this.clearingService.clearCancelOrder(order);
        this.orderBookChanged = true;
        // 发送成功消息
        this.apiResultQueue.add(ApiResultMessage.orderSuccess(event.refId, order, event.createTime));
//        this.notificationQueue.add(createNotification(event.createTime, "order_canceled", order.userId, order));
    }
    
    boolean transfer(TransferEvent event) {
        boolean ok = this.assetService.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, event.fromUserId, event.toUserId, event.asset, event.amount, event.sufficient);
        return ok;
    }
    
    void saveToDb() throws InterruptedException {
        if (!matchQueue.isEmpty()) {
            List<MatchDetailEntity> batch = new ArrayList<>(1000);
            for (; ; ) {
                List<MatchDetailEntity> matches = matchQueue.poll();
                if (matches != null) {
                    batch.addAll(matches);
                    if (batch.size() >= 1000) {
                        break;
                    }
                } else {
                    break;
                }
            }
            batch.sort(MatchDetailEntity::compareTo);
            if (logger.isDebugEnabled()) {
                logger.debug("batch insert {} match details...", batch.size());
            }
            this.storeService.insertIgnore(batch);
        }
        if (!orderQueue.isEmpty()) {
            List<OrderEntity> batch = new ArrayList<>(1000);
            for (; ; ) {
                List<OrderEntity> orders = orderQueue.poll();
                if (orders != null) {
                    batch.addAll(orders);
                    if (batch.size() >= 1000) {
                        break;
                    }
                } else {
                    break;
                }
            }
            batch.sort(OrderEntity::compareTo);
            if (logger.isDebugEnabled()) {
                logger.debug("batch update {} orders...", batch.size());
            }
        }
        if (matchQueue.isEmpty()) {
            Thread.sleep(1);
        }
    }
    
    MatchDetailEntity generateMatchDetailEntity(long sequenceId, long timeStamp, MatchDetailRecord detail, boolean forTaker) {
        MatchDetailEntity entity = new MatchDetailEntity();
        entity.sequenceId = sequenceId;
        entity.orderId = forTaker ? detail.takerOrder().id : detail.makerOrder().id;
        entity.counterOrderId = forTaker ? detail.makerOrder().id : detail.takerOrder().id;
        entity.direction = forTaker ? detail.takerOrder().direction : detail.makerOrder().direction;
        entity.price = detail.price();
        entity.quantity = detail.quantity();
        entity.type = forTaker ? MatchType.TAKER : MatchType.MAKER;
        entity.userId = forTaker ? detail.takerOrder().userId : detail.makerOrder().userId;
        entity.counterUserId = forTaker ? detail.makerOrder().userId : detail.takerOrder().userId;
        entity.createTime = timeStamp;
        return entity;
    }
    
    void validate() {
        logger.debug("start validate...");
        validateAssets();
        validateOrders();
        validateMatchEngine();
        logger.debug("validate ok.");
    }
    
    void validateAssets() {
        // 验证系统资产完整性
        BigDecimal totalUSD = BigDecimal.ZERO;
        BigDecimal totalBTC = BigDecimal.ZERO;
        for (Map.Entry<Long, ConcurrentMap<AssetEnum, Asset>> userEntry : this.assetService.getUserAssets().entrySet()) {
            Long userId = userEntry.getKey();
            ConcurrentMap<AssetEnum, Asset> assets = userEntry.getValue();
            for (Map.Entry<AssetEnum, Asset> entry : assets.entrySet()) {
                AssetEnum assetId = entry.getKey();
                Asset asset = entry.getValue();
                if (userId.longValue() == UserType.DEBT.getInternalUserId()) {
                    // 系统负债账户available不允许为正:
                    require(asset.getAvailable().signum() <= 0, "Debt has positive available: " + asset);
                    // 系统负债账户frozen必须为0:
                    require(asset.getFrozen().signum() == 0, "Debt has non-zero frozen: " + asset);
                } else {
                    // 交易用户的available/frozen不允许为负数:
                    require(asset.getAvailable().signum() >= 0, "Trader has negative available: " + asset);
                    require(asset.getFrozen().signum() >= 0, "Trader has negative frozen: " + asset);
                }
                switch (assetId) {
                    case USD -> totalUSD = totalUSD.add(asset.getTotal());
                    case BTC -> totalBTC = totalBTC.add(asset.getTotal());
                    default -> require(false, "Unexpected asset id: " + assetId);
                }
            }
        }
        // 各类别资产总额为0:
        require(totalUSD.signum() == 0, "Non zero USD balance: " + totalUSD);
        require(totalBTC.signum() == 0, "Non zero BTC balance: " + totalBTC);
    }
    
    void validateOrders() {
        // 验证订单
        Map<Long, Map<AssetEnum, BigDecimal>> userOrderFrozen = new HashMap<>();
        for (Map.Entry<Long, OrderEntity> entry : this.orderService.getActiveOrders().entrySet()) {
            OrderEntity order = entry.getValue();
            require(order.unfilledQuantity.signum() > 0, "Active order must have positive unfilled amount: " + order);
            switch (order.direction) {
                case BUY -> {
                    // 订单必须在MatchEngine中:
                    require(this.matchEngine.buyBook.exist(order), "order not found in buy book: " + order);
                    // 累计冻结的USD:
                    userOrderFrozen.putIfAbsent(order.userId, new HashMap<>());
                    Map<AssetEnum, BigDecimal> frozenAssets = userOrderFrozen.get(order.userId);
                    frozenAssets.putIfAbsent(AssetEnum.USD, BigDecimal.ZERO);
                    BigDecimal frozen = frozenAssets.get(AssetEnum.USD);
                    frozenAssets.put(AssetEnum.USD, frozen.add(order.price.multiply(order.unfilledQuantity)));
                }
                case SELL -> {
                    // 订单必须在MatchEngine中:
                    require(this.matchEngine.sellBook.exist(order), "order not found in sell book: " + order);
                    // 累计冻结的BTC:
                    userOrderFrozen.putIfAbsent(order.userId, new HashMap<>());
                    Map<AssetEnum, BigDecimal> frozenAssets = userOrderFrozen.get(order.userId);
                    frozenAssets.putIfAbsent(AssetEnum.BTC, BigDecimal.ZERO);
                    BigDecimal frozen = frozenAssets.get(AssetEnum.BTC);
                    frozenAssets.put(AssetEnum.BTC, frozen.add(order.unfilledQuantity));
                }
                default -> require(false, "Unexpected order direction: " + order.direction);
            }
        }
        // 订单冻结的累计金额必须和Asset冻结一致:
        for (Entry<Long, ConcurrentMap<AssetEnum, Asset>> userEntry : this.assetService.getUserAssets().entrySet()) {
            Long userId = userEntry.getKey();
            ConcurrentMap<AssetEnum, Asset> assets = userEntry.getValue();
            for (Entry<AssetEnum, Asset> entry : assets.entrySet()) {
                AssetEnum assetId = entry.getKey();
                Asset asset = entry.getValue();
                if (asset.getFrozen().signum() > 0) {
                    Map<AssetEnum, BigDecimal> orderFrozen = userOrderFrozen.get(userId);
                    require(orderFrozen != null, "No order frozen found for user: " + userId + ", asset: " + asset);
                    BigDecimal frozen = orderFrozen.get(assetId);
                    require(frozen != null, "No order frozen found for asset: " + asset);
                    require(frozen.compareTo(asset.getFrozen()) == 0,
                            "Order frozen " + frozen + " is not equals to asset frozen: " + asset);
                    // 从userOrderFrozen中删除已验证的Asset数据:
                    orderFrozen.remove(assetId);
                }
            }
        }
        // userOrderFrozen不存在未验证的Asset数据:
        for (Entry<Long, Map<AssetEnum, BigDecimal>> userEntry : userOrderFrozen.entrySet()) {
            Long userId = userEntry.getKey();
            Map<AssetEnum, BigDecimal> frozenAssets = userEntry.getValue();
            require(frozenAssets.isEmpty(), "User " + userId + " has unexpected frozen for order: " + frozenAssets);
        }
    }
    
    void validateMatchEngine() {
        // OrderBook的Order必须在ActiveOrders中:
        Map<Long, OrderEntity> copyOfActiveOrders = new HashMap<>(this.orderService.getActiveOrders());
        for (OrderEntity order : this.matchEngine.buyBook.book.values()) {
            require(copyOfActiveOrders.remove(order.id) == order,
                    "Order in buy book is not in active orders: " + order);
        }
        for (OrderEntity order : this.matchEngine.sellBook.book.values()) {
            require(copyOfActiveOrders.remove(order.id) == order,
                    "Order in sell book is not in active orders: " + order);
        }
        // activeOrders的所有Order必须在Order Book中:
        require(copyOfActiveOrders.isEmpty(), "Not all active orders are in order book.");
    }
    
    public void debug() {
        System.out.println("========== trading engine ==========");
        this.assetService.debug();
        this.orderService.debug();
        this.matchEngine.debug();
        System.out.println("========== // trading engine ==========");
    }
    
    void panic() {
        logger.error("Application panic. Exit now...");
        this.fatalError = true;
        System.exit(1);
    }
    
    void require(boolean condition, String errorMessage) {
        if (!condition) {
            logger.error("validate failed: {}", errorMessage);
            panic();
        }
    }
}