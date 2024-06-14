package com.warp.exchange.quatation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.warp.exchange.entity.quatation.*;
import com.warp.exchange.enums.BarType;
import com.warp.exchange.message.AbstractMessage;
import com.warp.exchange.message.TickMessage;
import com.warp.exchange.messaging.MessageConsumer;
import com.warp.exchange.messaging.Messaging;
import com.warp.exchange.messaging.MessagingFactory;
import com.warp.exchange.redis.RedisCache;
import com.warp.exchange.redis.RedisService;
import com.warp.exchange.support.AbstractBarEntity;
import com.warp.exchange.support.LoggerSupport;
import com.warp.exchange.util.IpUtil;
import com.warp.exchange.util.JsonUtil;
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

@Component
public class QuotationService extends LoggerSupport {
    
    static final TypeReference<Map<BarType, BigDecimal[]>> TYPE_BARS = new TypeReference<>() {
    };
    
    @Autowired
    ZoneId zoneId;
    
    @Autowired
    RedisService redisService;
    
    @Autowired
    QuotationDbService quotationDbService;
    
    @Autowired
    MessagingFactory messagingFactory;
    
    MessageConsumer tickConsumer;
    
    String shaUpdateRecentTicksLua = null;
    
    String shaUpdateBarLua = null;
    
    long sequenceId;
    
    // TODO
    static <T extends AbstractBarEntity> T createBar(Supplier<T> fn, BigDecimal[] data) {
        if (data == null) {
            return null;
        }
        T bar = fn.get();
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
        // init mq
        String groupId = Messaging.Topic.TICK.name() + "_" + IpUtil.getHostId();
        this.tickConsumer = this.messagingFactory.createBatchMessageListener(Messaging.Topic.TICK, groupId, this::processMessages);
    }
    
    @PreDestroy
    public void shutdown() {
        if (this.tickConsumer != null) {
            this.tickConsumer.stop();
            this.tickConsumer = null;
        }
    }
    
    void processMessages(List<AbstractMessage> messages) {
        for (AbstractMessage message : messages) {
            processMessage((TickMessage) message);
        }
    }
    
    void processMessage(TickMessage message) {
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
                // open price is set:
                closePrice = tick.price;
                highPrice = highPrice.max(tick.price);
                lowPrice = lowPrice.min(tick.price);
            }
            quantity = quantity.add(tick.quantity);
        }
        
        long sec = createTime / 1000;
        long min = sec / 60;
        long hour = min / 60;
        long secStartTime = sec * 1000; // 秒K的开始时间
        long minStartTime = min * 60 * 1000; // 分钟K的开始时间
        long hourStartTime = hour * 3600 * 1000; // 小时K的开始时间
        long dayStartTime = Instant.ofEpochMilli(hourStartTime).atZone(zoneId).withHour(0).toEpochSecond() * 1000; // 日K的开始时间，与TimeZone相关
        
        // 更新Redis最近的Ticks缓存:
        String ticksData = ticksJoiner.toString();
        if (logger.isDebugEnabled()) {
            logger.debug("generated ticks data: {}", ticksData);
        }
        Boolean tickOk = redisService.executeScriptReturnBoolean(this.shaUpdateRecentTicksLua,
                new String[]{RedisCache.Key.RECENT_TICKS},
                new String[]{String.valueOf(this.sequenceId), ticksData, ticksStrJoiner.toString()});
        if (!tickOk.booleanValue()) {
            logger.warn("ticks are ignored by Redis.");
        }
        // 保存Tick至数据库:
        this.quotationDbService.saveTicks(message.ticks);
        
        // 更新各种类型的K线:
        String strCreatedBars = redisService.executeScriptReturnString(this.shaUpdateBarLua,
                new String[]{RedisCache.Key.SEC_BARS, RedisCache.Key.MIN_BARS, RedisCache.Key.HOUR_BARS,
                        RedisCache.Key.DAY_BARS},
                new String[]{ // ARGV
                        String.valueOf(this.sequenceId), // sequence id
                        String.valueOf(secStartTime), // sec-start-time
                        String.valueOf(minStartTime), // min-start-time
                        String.valueOf(hourStartTime), // hour-start-time
                        String.valueOf(dayStartTime), // day-start-time
                        String.valueOf(openPrice), // open
                        String.valueOf(highPrice), // high
                        String.valueOf(lowPrice), // low
                        String.valueOf(closePrice), // close
                        String.valueOf(quantity) // quantity
                });
        logger.info("returned created bars: " + strCreatedBars);
        // 将Redis返回的K线保存至数据库:
        Map<BarType, BigDecimal[]> barMap = JsonUtil.readJson(strCreatedBars, TYPE_BARS);
        if (!barMap.isEmpty()) {
            SecBarEntity secBar = createBar(SecBarEntity::new, barMap.get(BarType.SEC));
            MinBarEntity minBar = createBar(MinBarEntity::new, barMap.get(BarType.MIN));
            HourBarEntity hourBar = createBar(HourBarEntity::new, barMap.get(BarType.HOUR));
            DayBarEntity dayBar = createBar(DayBarEntity::new, barMap.get(BarType.DAY));
            this.quotationDbService.saveBars(secBar, minBar, hourBar, dayBar);
        }
    }
}
