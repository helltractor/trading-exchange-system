package com.helltractor.exchange;

import com.helltractor.exchange.assets.Asset;
import com.helltractor.exchange.assets.AssetService;
import com.helltractor.exchange.assets.Transfer;
import com.helltractor.exchange.bean.OrderBookBean;
import com.helltractor.exchange.clearing.ClearingService;
import com.helltractor.exchange.enums.AssetEnum;
import com.helltractor.exchange.enums.Direction;
import com.helltractor.exchange.enums.MatchType;
import com.helltractor.exchange.enums.UserType;
import com.helltractor.exchange.match.MatchDetailRecord;
import com.helltractor.exchange.match.MatchEngine;
import com.helltractor.exchange.match.MatchResult;
import com.helltractor.exchange.message.ApiResultMessage;
import com.helltractor.exchange.message.NotificationMessage;
import com.helltractor.exchange.message.TickMessage;
import com.helltractor.exchange.message.event.AbstractEvent;
import com.helltractor.exchange.message.event.OrderCancelEvent;
import com.helltractor.exchange.message.event.OrderRequestEvent;
import com.helltractor.exchange.message.event.TransferEvent;
import com.helltractor.exchange.messaging.MessageConsumer;
import com.helltractor.exchange.messaging.MessageProducer;
import com.helltractor.exchange.messaging.Messaging;
import com.helltractor.exchange.messaging.MessagingFactory;
import com.helltractor.exchange.model.quatation.TickEntity;
import com.helltractor.exchange.model.trade.MatchDetailEntity;
import com.helltractor.exchange.model.trade.OrderEntity;
import com.helltractor.exchange.order.OrderService;
import com.helltractor.exchange.redis.RedisCache;
import com.helltractor.exchange.redis.RedisService;
import com.helltractor.exchange.store.StoreService;
import com.helltractor.exchange.support.LoggerSupport;
import com.helltractor.exchange.util.IpUtil;
import com.helltractor.exchange.util.JsonUtil;
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
    private ZoneId zoneId = ZoneId.systemDefault();
    
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
    
    @Value("#{exchangeConfiguration.orderBookDepth}")
    private final int orderBookDepth = 100;
    
    @Value("#{exchangeConfiguration.debugMode}")
    private boolean debugMode = false;
    
    private boolean fatalError = false;
    private MessageConsumer consumer;
    private MessageProducer<TickMessage> producer;
    private long lastSequenceId = 0;
    private boolean orderBookChanged = false;
    private String shaUpdateOrderBookLua;
    
    private Thread tickThread;
    private Thread notifyThread;
    private Thread apiResultThread;
    private Thread orderBookThread;
    private Thread dbThread;
    
    private OrderBookBean latestOrderBook = null;
    private final Queue<List<OrderEntity>> orderQueue = new ConcurrentLinkedQueue<>();
    private final Queue<List<MatchDetailEntity>> matchQueue = new ConcurrentLinkedQueue<>();
    private final Queue<TickMessage> tickQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ApiResultMessage> apiResultQueue = new ConcurrentLinkedQueue<>();
    private final Queue<NotificationMessage> notificationQueue = new ConcurrentLinkedQueue<>();
    
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
    
    private void runTickThread() {
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
                // 无推送时，暂停1ms
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }
    
    private void runNotifyThread() {
        logger.info("start publish notify to redis...");
        for (; ; ) {
            NotificationMessage message = this.notificationQueue.poll();
            if (message != null) {
                redisService.publish(RedisCache.Topic.NOTIFICATION, JsonUtil.writeJson(message));
            } else {
                // 无推送时，暂停1ms
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }
    
    private void runOrderBookThread() {
        logger.info("start update orderbook snapshot to redis...");
        long lastSequenceId = 0;
        for (; ; ) {
            // 获取OrderBookBean的引用，确保后续操作针对局部变量而非成员变量
            final OrderBookBean orderBook = this.latestOrderBook;
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
    
    private void runApiResultThread() {
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
    
    private void runDbThread() {
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
    
    public void processMessages(List<AbstractEvent> messages) {
        for (AbstractEvent message : messages) {
            processEvent(message);
        }
        if (this.orderBookChanged) {
            // 获取最新的OrderBook快照
            this.latestOrderBook = this.matchEngine.getOrderBook(this.orderBookDepth);
        }
    }
    
    public void processEvent(AbstractEvent event) {
        if (this.fatalError) {
            return;
        }
        // 判断是否重复消息
        if (event.sequenceId <= this.lastSequenceId) {
            logger.warn("skip duplicated event: {}", event);
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
            this.debug();
        }
    }
    
    private void createOrder(OrderRequestEvent event) {
        // 创建订单ID
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.createTime), zoneId);
        int year = zonedDateTime.getYear();
        int month = zonedDateTime.getMonthValue();
        long orderId = event.sequenceId * 10000 + (year * 100L + month);
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
        // 收集已完成的OrderEntity并生成MatchDetailEntity, TickEntity
        if (!result.matchDetails.isEmpty()) {
            List<OrderEntity> closedOrders = new ArrayList<>();
            List<MatchDetailEntity> matchDetails = new ArrayList<>();
            List<TickEntity> ticks = new ArrayList<>();
            if (result.takerOrder.status.isFinalStatus) {
                closedOrders.add(result.takerOrder);
            }
            for (MatchDetailRecord detail : result.matchDetails) {
                OrderEntity makerOrder = detail.makerOrder();
                notifications.add(createNotification(event.createTime, "order_matched", makerOrder.userId, makerOrder.copy()));
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
    
    private NotificationMessage createNotification(long ts, String type, Long userId, Object data) {
        NotificationMessage msg = new NotificationMessage();
        msg.createTime = ts;
        msg.type = type;
        msg.userId = userId;
        msg.data = data;
        return msg;
    }
    
    private void cancelOrder(OrderCancelEvent event) {
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
        this.notificationQueue.add(createNotification(event.createTime, "order_canceled", order.userId, order));
    }
    
    private boolean transfer(TransferEvent event) {
        return this.assetService.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, event.fromUserId, event.toUserId, event.asset, event.amount, event.sufficient);
    }
    
    private void saveToDb() throws InterruptedException {
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
                logger.debug("batch insert {} orders...", batch.size());
            }
            // 序列化
            this.storeService.insertIgnore(batch);
        }
        if (matchQueue.isEmpty()) {
            Thread.sleep(1);
        }
    }
    
    private MatchDetailEntity generateMatchDetailEntity(long sequenceId, long timeStamp, MatchDetailRecord detail, boolean forTaker) {
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
    
    private void validateAssets() {
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
                    // 系统负债账户available不允许为正
                    require(asset.getAvailable().signum() <= 0, "Debt has positive available: " + asset);
                    // 系统负债账户frozen必须为0
                    require(asset.getFrozen().signum() == 0, "Debt has non-zero frozen: " + asset);
                } else {
                    // 交易用户的available/frozen不允许为负数
                    require(asset.getAvailable().signum() >= 0, "Trader has negative available: " + asset);
                    require(asset.getFrozen().signum() >= 0, "Trader has negative frozen: " + asset);
                }
                switch (assetId) {
                    case USD -> totalUSD = totalUSD.add(asset.getTotal());
                    case BTC -> totalBTC = totalBTC.add(asset.getTotal());
                    default -> require(false, "Unexpected assets id: " + assetId);
                }
            }
        }
        // 各类别资产总额为0
        require(totalUSD.signum() == 0, "Non zero USD balance: " + totalUSD);
        require(totalBTC.signum() == 0, "Non zero BTC balance: " + totalBTC);
    }
    
    private void validateOrders() {
        // 验证订单
        Map<Long, Map<AssetEnum, BigDecimal>> userOrderFrozen = new HashMap<>();
        for (Map.Entry<Long, OrderEntity> entry : this.orderService.getActiveOrders().entrySet()) {
            OrderEntity order = entry.getValue();
            require(order.unfilledQuantity.signum() > 0, "Active order must have positive unfilled amount: " + order);
            switch (order.direction) {
                case BUY -> {
                    // 订单必须在MatchEngine中
                    require(this.matchEngine.buyBook.exist(order), "order not found in buy book: " + order);
                    // 累计冻结的USD
                    userOrderFrozen.putIfAbsent(order.userId, new HashMap<>());
                    Map<AssetEnum, BigDecimal> frozenAssets = userOrderFrozen.get(order.userId);
                    frozenAssets.putIfAbsent(AssetEnum.USD, BigDecimal.ZERO);
                    BigDecimal frozen = frozenAssets.get(AssetEnum.USD);
                    frozenAssets.put(AssetEnum.USD, frozen.add(order.price.multiply(order.unfilledQuantity)));
                }
                case SELL -> {
                    // 订单必须在MatchEngine中
                    require(this.matchEngine.sellBook.exist(order), "order not found in sell book: " + order);
                    // 累计冻结的BTC
                    userOrderFrozen.putIfAbsent(order.userId, new HashMap<>());
                    Map<AssetEnum, BigDecimal> frozenAssets = userOrderFrozen.get(order.userId);
                    frozenAssets.putIfAbsent(AssetEnum.BTC, BigDecimal.ZERO);
                    BigDecimal frozen = frozenAssets.get(AssetEnum.BTC);
                    frozenAssets.put(AssetEnum.BTC, frozen.add(order.unfilledQuantity));
                }
                default -> require(false, "Unexpected order direction: " + order.direction);
            }
        }
        // 订单冻结的累计金额必须和Asset冻结一致
        for (Entry<Long, ConcurrentMap<AssetEnum, Asset>> userEntry : this.assetService.getUserAssets().entrySet()) {
            Long userId = userEntry.getKey();
            ConcurrentMap<AssetEnum, Asset> assets = userEntry.getValue();
            for (Entry<AssetEnum, Asset> entry : assets.entrySet()) {
                AssetEnum assetId = entry.getKey();
                Asset asset = entry.getValue();
                if (asset.getFrozen().signum() > 0) {
                    Map<AssetEnum, BigDecimal> orderFrozen = userOrderFrozen.get(userId);
                    require(orderFrozen != null, "No order frozen found for user: " + userId + ", assets: " + asset);
                    BigDecimal frozen = orderFrozen.get(assetId);
                    require(frozen != null, "No order frozen found for assets: " + asset);
                    require(frozen.compareTo(asset.getFrozen()) == 0,
                            "Order frozen " + frozen + " is not equals to assets frozen: " + asset);
                    // 从userOrderFrozen中删除已验证的Asset数据
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
    
    private void validateMatchEngine() {
        // OrderBook的Order必须在ActiveOrders中
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
    
    private void panic() {
        logger.error("Application panic. Exit now...");
        this.fatalError = true;
        System.exit(1);
    }
    
    private void require(boolean condition, String errorMessage) {
        if (!condition) {
            logger.error("validate failed: {}", errorMessage);
            panic();
        }
    }
}
