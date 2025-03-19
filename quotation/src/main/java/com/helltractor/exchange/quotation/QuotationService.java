package com.helltractor.exchange.quotation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.helltractor.exchange.enums.BarType;
import com.helltractor.exchange.message.AbstractMessage;
import com.helltractor.exchange.message.TickMessage;
import com.helltractor.exchange.messaging.MessageConsumer;
import com.helltractor.exchange.messaging.Messaging;
import com.helltractor.exchange.messaging.MessagingFactory;
import com.helltractor.exchange.model.quatation.TickEntity;
import com.helltractor.exchange.model.quatation.DayBarEntity;
import com.helltractor.exchange.model.quatation.HourBarEntity;
import com.helltractor.exchange.model.quatation.MinBarEntity;
import com.helltractor.exchange.model.quatation.SecBarEntity;
import com.helltractor.exchange.model.support.AbstractBarEntity;
import com.helltractor.exchange.redis.RedisCache;
import com.helltractor.exchange.redis.RedisService;
import com.helltractor.exchange.support.LoggerSupport;
import com.helltractor.exchange.util.IpUtil;
import com.helltractor.exchange.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Supplier;

/**
 * Quotation service.
 */
@Component
public class QuotationService extends LoggerSupport {
    
    private static final TypeReference<Map<BarType, BigDecimal[]>> TYPE_BARS = new TypeReference<>() {
    };
    
    @Autowired
    private ZoneId zoneId;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private QuotationDbService quotationDbService;
    
    @Autowired
    private MessagingFactory messagingFactory;
    
    private MessageConsumer tickConsumer;
    
    private String shaUpdateRecentTicksLua = null;
    
    private String shaUpdateBarLua = null;
    
    private long sequenceId;
    
    private static <T extends AbstractBarEntity> T createBar(Supplier<T> supplier, BigDecimal[] data) {
        if (data == null) {
            return null;
        }
        T bar = supplier.get();
        bar.startTime = data[0].longValue();
        bar.openPrice = data[1];
        bar.highPrice = data[2];
        bar.lowPrice = data[3];
        bar.closePrice = data[4];
        bar.quantity = data[5];
        return bar;
    }
    
    @PostConstruct
    public void init() {
        // init redis lua script
        this.shaUpdateRecentTicksLua = this.redisService.loadScriptFromClassPath("/redis/update-recent-ticks.lua");
        this.shaUpdateBarLua = this.redisService.loadScriptFromClassPath("/redis/update-bar.lua");
        // init message queue
        String groupId = Messaging.Topic.TICK.name() + "_" + IpUtil.getHostId();
        this.tickConsumer = this.messagingFactory.createBatchMessageListener(Messaging.Topic.TICK, groupId,
                this::processMessages);
    }
    
    @PreDestroy
    public void shutdown() {
        if (this.tickConsumer != null) {
            this.tickConsumer.stop();
            this.tickConsumer = null;
        }
    }
    
    private void processMessages(List<AbstractMessage> messages) {
        for (AbstractMessage message : messages) {
            processMessage((TickMessage) message);
        }
    }
    
    private void processMessage(TickMessage message) {
        // ignore repeated messages
        if (message.sequenceId < this.sequenceId) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("process ticks: sequenceId = {}, {} ticks...", message.sequenceId, message.ticks.size());
        }
        
        this.sequenceId = message.sequenceId;
        final long createTime = message.createTime;
        StringJoiner ticksStrJoiner = new StringJoiner(",", "[", "]");
        StringJoiner ticksJoiner = new StringJoiner(",", "[", "]");
        BigDecimal openPrice = BigDecimal.ZERO;
        BigDecimal closePrice = BigDecimal.ZERO;
        BigDecimal highPrice = BigDecimal.ZERO;
        BigDecimal lowPrice = BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.ZERO;
        for (TickEntity tick : message.ticks) {
            String json = tick.toJson();
            ticksStrJoiner.add("\"" + json + "\"");
            ticksJoiner.add(json);
            if (openPrice.signum() == 0) {
                openPrice = tick.price;
                closePrice = tick.price;
                highPrice = tick.price;
                lowPrice = tick.price;
            } else {
                closePrice = tick.price;
                highPrice = highPrice.max(tick.price);
                lowPrice = lowPrice.min(tick.price);
            }
            quantity = quantity.add(tick.quantity);
        }
        
        long sec = createTime / 1000;
        long min = sec / 60;
        long hour = min / 60;
        long secStartTime = sec * 1000; // K seconds start time
        long minStartTime = min * 60 * 1000; // K minutes start time
        long hourStartTime = hour * 3600 * 1000; // K hours start time
        long dayStartTime = Instant.ofEpochMilli(hourStartTime).atZone(zoneId).withHour(0).toEpochSecond() * 1000; // 日K的开始时间，与TimeZone相关
        // update recent ticks in Redis
        String ticksData = ticksJoiner.toString();
        if (logger.isDebugEnabled()) {
            logger.debug("generated ticks data: {}", ticksData);
        }
        Boolean tickOk = redisService.executeScriptReturnBoolean(this.shaUpdateRecentTicksLua,
                // KEYS
                new String[]{RedisCache.Key.RECENT_TICKS},
                // ARGV
                new String[]{String.valueOf(this.sequenceId), ticksData, ticksStrJoiner.toString()});
        if (!tickOk.booleanValue()) {
            logger.warn("ticks are ignored by Redis.");
            return;
        }
        // save ticks to database
        this.quotationDbService.saveTicks(message.ticks);
        // update bars in Redis
        String strCreatedBars = redisService.executeScriptReturnString(this.shaUpdateBarLua,
                // KEYS
                new String[]{
                        RedisCache.Key.SEC_BARS,
                        RedisCache.Key.MIN_BARS,
                        RedisCache.Key.HOUR_BARS,
                        RedisCache.Key.DAY_BARS
                },
                // ARGV
                new String[]{
                        String.valueOf(this.sequenceId),
                        String.valueOf(secStartTime),
                        String.valueOf(minStartTime),
                        String.valueOf(hourStartTime),
                        String.valueOf(dayStartTime),
                        String.valueOf(openPrice),
                        String.valueOf(highPrice),
                        String.valueOf(lowPrice),
                        String.valueOf(closePrice),
                        String.valueOf(quantity)
                });
        
        logger.info("returned created bars: {}", strCreatedBars);
        // save bars to database from Redis
        Map<BarType, BigDecimal[]> barTypeMap = JsonUtil.readJson(strCreatedBars, TYPE_BARS);
        if (!barTypeMap.isEmpty()) {
            SecBarEntity secBar = createBar(SecBarEntity::new, barTypeMap.get(BarType.SEC));
            MinBarEntity minBar = createBar(MinBarEntity::new, barTypeMap.get(BarType.MIN));
            HourBarEntity hourBar = createBar(HourBarEntity::new, barTypeMap.get(BarType.HOUR));
            DayBarEntity dayBar = createBar(DayBarEntity::new, barTypeMap.get(BarType.DAY));
            this.quotationDbService.saveBars(secBar, minBar, hourBar, dayBar);
        }
    }
}
