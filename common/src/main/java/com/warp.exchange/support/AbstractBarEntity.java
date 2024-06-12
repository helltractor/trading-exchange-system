package com.warp.exchange.support;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * 存储K线数据的实体类的抽象类
 * 存储秒，分钟，小时，日的柱状数据
 */
// @MappedSuperclass用于标记一个类，让它的映射信息能够被子类继承。这个注解的类本身并不会映射到数据库表，但它的属性会映射到其子类对应的数据库表字段
@MappedSuperclass
public class AbstractBarEntity implements EntitySupport {
    
    static final Map<String, DateTimeFormatter> FORMATTERS = Map.of( //
            "SecBarEntity", DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US), //
            "MinBarEntity", DateTimeFormatter.ofPattern("dd HH:mm", Locale.US), //
            "HourBarEntity", DateTimeFormatter.ofPattern("MM-dd HH", Locale.US), //
            "DayBarEntity", DateTimeFormatter.ofPattern("yy-MM-dd", Locale.US));
    /**
     * start timestamp in millisecond (included).
     */
    @Id
    @Column(nullable = false, updatable = false)
    public long startTime;
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal openPrice;
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal highPrice;
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal lowPrice;
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal closePrice;
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal quantity;
    
    /**
     * Get compact bar data as array: [startTime, O, H, L, C, V].
     *
     * @return Number[] array contains 6 elements.
     */
    public Number[] getBarData() {
        return new Number[]{startTime, openPrice, highPrice, lowPrice, closePrice, quantity};
    }
    
    public String toString(ZoneId zoneId) {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.startTime), zoneId);
        String time = FORMATTERS.get(getClass().getSimpleName()).format(zonedDateTime);
        return String.format("{%s: startTime=%s, O=%s, H=%s, L=%s, C=%s, qty=%s}", this.getClass().getSimpleName(),
                time, this.openPrice, this.highPrice, this.lowPrice, this.closePrice, this.quantity);
    }
}
