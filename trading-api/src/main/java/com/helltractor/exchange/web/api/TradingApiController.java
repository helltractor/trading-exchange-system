package com.helltractor.exchange.web.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helltractor.exchange.ApiError;
import com.helltractor.exchange.ApiErrorResponse;
import com.helltractor.exchange.ApiException;
import com.helltractor.exchange.bean.OrderBookBean;
import com.helltractor.exchange.bean.OrderRequestBean;
import com.helltractor.exchange.bean.SimpleMatchDetailRecord;
import com.helltractor.exchange.ctx.UserContext;
import com.helltractor.exchange.message.ApiResultMessage;
import com.helltractor.exchange.message.event.OrderCancelEvent;
import com.helltractor.exchange.message.event.OrderRequestEvent;
import com.helltractor.exchange.model.trade.OrderEntity;
import com.helltractor.exchange.redis.RedisCache;
import com.helltractor.exchange.redis.RedisService;
import com.helltractor.exchange.service.HistoryService;
import com.helltractor.exchange.service.SendEventService;
import com.helltractor.exchange.service.TradingEngineApiProxyService;
import com.helltractor.exchange.support.LoggerSupport;
import com.helltractor.exchange.util.IdUtil;
import com.helltractor.exchange.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class TradingApiController extends LoggerSupport {
    
    Map<String, DeferredResult<ResponseEntity<String>>> deferredResultMap = new ConcurrentHashMap<>();
    
    @Autowired
    HistoryService historyService;
    
    @Autowired
    SendEventService sendEventService;
    
    @Autowired
    RedisService redisService;
    
    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    TradingEngineApiProxyService tradingEngineApiProxyService;
    
    private final Long asyncTimeout = Long.valueOf(500);
    
    private String timeoutJson = null;
    
    private String getTimeoutJson() throws JsonProcessingException {
        if (timeoutJson == null) {
            timeoutJson = this.objectMapper.writeValueAsString(new ApiErrorResponse(ApiError.OPERATION_TIMEOUT, null, ""));
        }
        return timeoutJson;
    }
    
    @PostConstruct
    public void init() {
        this.redisService.subscribe(RedisCache.Topic.TRADING_API_RESULT, this::onTradingApiResult);
    }
    
    @GetMapping("/timestamp")
    public Map<String, Long> timestamp() {
        return Map.of("timestamp", Long.valueOf(System.currentTimeMillis()));
    }
    
    @ResponseBody
    @GetMapping(value = "/assets", produces = "application/json")
    public String getAssets() throws IOException {
        return tradingEngineApiProxyService.get("/internal/" + UserContext.getRequiredUserId() + "/assets");
    }
    
    @ResponseBody
    @GetMapping(value = "/orders/{orderId}", produces = "application/json")
    public String getOpenOrder(@PathVariable("orderId") Long orderId) throws IOException {
        final Long userId = UserContext.getRequiredUserId();
        return tradingEngineApiProxyService.get("/internal/" + userId + "/orders/" + orderId);
    }
    
    @ResponseBody
    @GetMapping(value = "/orders", produces = "application/json")
    public String getOpenOrders() throws IOException {
        return tradingEngineApiProxyService.get("/internal/" + UserContext.getRequiredUserId() + "/orders");
    }
    
    @ResponseBody
    @GetMapping(value = "/orderBook", produces = "application/json")
    public String getOrderBook() {
        String data = redisService.get(RedisCache.Key.ORDER_BOOK);
        return data == null ? OrderBookBean.EMPTY : data;
    }
    
    @ResponseBody
    @GetMapping(value = "/ticks", produces = "application/json")
    public String getRecentTicks() {
        List<String> data = redisService.lrange(RedisCache.Key.RECENT_TICKS, 0, -1);
        if (data == null || data.isEmpty()) {
            return "[]";
        }
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (String t : data) {
            sj.add(t);
        }
        return sj.toString();
    }
    
    @ResponseBody
    @GetMapping(value = "/bars/day", produces = "application/json")
    public String getDayBars() {
        long end = System.currentTimeMillis();
        long start = end - 366L * 86400_000;
        return getBars(RedisCache.Key.DAY_BARS, start, end);
    }
    
    @ResponseBody
    @GetMapping(value = "/bars/hour", produces = "application/json")
    public String getHourBars() {
        long end = System.currentTimeMillis();
        long start = end - 720L * 3600_000;
        return getBars(RedisCache.Key.HOUR_BARS, start, end);
    }
    
    @ResponseBody
    @GetMapping(value = "/bars/min", produces = "application/json")
    public String getMinBars() {
        long end = System.currentTimeMillis();
        long start = end - 1440 * 60_000;
        return getBars(RedisCache.Key.MIN_BARS, start, end);
    }
    
    @ResponseBody
    @GetMapping(value = "/bars/sec", produces = "application/json")
    public String getSecBars() {
        long end = System.currentTimeMillis();
        long start = end - 3600 * 1_000;
        return getBars(RedisCache.Key.SEC_BARS, start, end);
    }
    
    private String getBars(String key, long start, long end) {
        List<String> data = redisService.zrangebyscore(key, start, end);
        if (data == null || data.isEmpty()) {
            return "[]";
        }
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (String t : data) {
            sj.add(t);
        }
        return sj.toString();
    }
    
    @GetMapping("/history/orders")
    public List<OrderEntity> getHistoryOrders(
            @RequestParam(value = "maxResults", defaultValue = "100") int maxResults) {
        if (maxResults < 1 || maxResults > 1000) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "maxResults", "Invalid parameter.");
        }
        return historyService.getHistoryOrders(UserContext.getRequiredUserId(), maxResults);
    }
    
    @GetMapping("/history/orders/{orderId}/matches")
    public List<SimpleMatchDetailRecord> getOrderMatchDetails(@PathVariable("orderId") Long orderId) throws Exception {
        final Long userId = UserContext.getRequiredUserId();
        // search active order
        String strOpenOrder = tradingEngineApiProxyService.get("/internal/" + userId + "/orders/" + orderId);
        if (strOpenOrder.equals("null")) {
            // search history order
            OrderEntity orderEntity = this.historyService.getHistoryOrder(userId, orderId);
            if (orderEntity == null) {
                // order not found
                throw new ApiException(ApiError.ORDER_NOT_FOUND, orderId.toString(), "Order not found.");
            }
        }
        return this.historyService.getHistoryMatchDetails(orderId);
    }
    
    @PostMapping(value = "/orders/{orderId}/cancel", produces = "application/json")
    @ResponseBody
    public DeferredResult<ResponseEntity<String>> cancelOrder(@PathVariable("orderId") Long orderId) throws Exception {
        final Long userId = UserContext.getRequiredUserId();
        String orderStr = tradingEngineApiProxyService.get("/internal/" + userId + "/orders/" + orderId);
        if (orderStr.equals("null")) {
            throw new ApiException(ApiError.ORDER_NOT_FOUND, orderId.toString(), "Active order not found.");
        }
        final String refId = IdUtil.generateUniqueId();
        var message = new OrderCancelEvent();
        message.refId = refId;
        message.refOrderId = orderId;
        message.userId = userId;
        message.createTime = System.currentTimeMillis();
        ResponseEntity<String> timeout = new ResponseEntity<>(getTimeoutJson(), HttpStatus.BAD_REQUEST);
        DeferredResult<ResponseEntity<String>> deferred = new DeferredResult<>(this.asyncTimeout, timeout);
        deferred.onTimeout(() -> {
            logger.warn("deferred order {} cancel request refId={} timeout.", orderId, refId);
            this.deferredResultMap.remove(refId);
        });
        // track deferred
        this.deferredResultMap.put(refId, deferred);
        logger.info("cancel order message created: {}", message);
        this.sendEventService.sendMessage(message);
        return deferred;
    }
    
    @PostMapping(value = "/orders", produces = "application/json")
    @ResponseBody
    public DeferredResult<ResponseEntity<String>> createOrder(@RequestBody OrderRequestBean orderRequest)
            throws IOException {
        final Long userId = UserContext.getRequiredUserId();
        orderRequest.validate();
        final String refId = IdUtil.generateUniqueId();
        var event = new OrderRequestEvent();
        event.refId = refId;
        event.userId = userId;
        event.direction = orderRequest.direction;
        event.price = orderRequest.price;
        event.quantity = orderRequest.quantity;
        event.createTime = System.currentTimeMillis();
        
        ResponseEntity<String> timeout = new ResponseEntity<>(getTimeoutJson(), HttpStatus.BAD_REQUEST);
        DeferredResult<ResponseEntity<String>> deferred = new DeferredResult<>(this.asyncTimeout, timeout);
        deferred.onTimeout(() -> {
            logger.warn("deferred order request refId = {} timeout.", event.refId);
            this.deferredResultMap.remove(event.refId);
        });
        // track deferred
        this.deferredResultMap.put(event.refId, deferred);
        this.sendEventService.sendMessage(event);
        return deferred;
    }
    
    
    public void onTradingApiResult(String msg) {
        logger.info("on subscribed msg: {}", msg);
        try {
            ApiResultMessage message = this.objectMapper.readValue(msg, ApiResultMessage.class);
            if (message.refId != null) {
                DeferredResult<ResponseEntity<String>> deferredResult = this.deferredResultMap.remove(message.refId);
                if (deferredResult != null) {
                    if (message.error != null) {
                        String error = objectMapper.writeValueAsString(message.error);
                        ResponseEntity<String> responseEntity = new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
                        deferredResult.setResult(responseEntity);
                    } else {
                        ResponseEntity<String> responseEntity = new ResponseEntity<>(JsonUtil.writeJson(message.result), HttpStatus.OK);
                        deferredResult.setResult(responseEntity);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Invalid ApiResultMessage: {}", msg, e);
        }
    }
}
