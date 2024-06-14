package com.helltractor.exchange.entity.trade.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.helltractor.exchange.enums.Direction;
import com.helltractor.exchange.enums.OrderStatus;
import com.helltractor.exchange.support.EntitySupport;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Order entity.
 */
@Entity
@Table(name = "orders")
public class OrderEntity implements EntitySupport, Comparable<OrderEntity> {

    /**
     * 主键，订单ID
     */
    @Id
    @Column(nullable = false, updatable = false)
    public Long id;

    /**
     * 用户ID
     */
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * 交易对ID
     */
//    @Column(nullable = false, updatable = false)
//    public Long symbolId;

    /**
     * 序列ID
     */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * 价格
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal price;

    /**
     * 买卖方向
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public Direction direction;

    /**
     * 订单状态
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public OrderStatus status;

    /**
     * 订单数量
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal quantity;

    /**
     * 未成交数量
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal unfilledQuantity; // 未成交数量

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    public long createTime;

    /**
     * 更新时间
     */
    @Column(nullable = false, updatable = false)
    public long updateTime;
    
    private int version;

    @Transient
    @JsonIgnore
    public int getVersion() {
        return this.version;
    }

    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus status, long timeStamp) {
        this.status = status;
        this.unfilledQuantity = unfilledQuantity;
        this.updateTime = timeStamp;
        this.version++;
    }

    @Nullable
    public OrderEntity copy() {
        OrderEntity order = new OrderEntity();
        int version = this.version;
        order.status = this.status;
        order.unfilledQuantity = this.unfilledQuantity;
        order.updateTime = this.updateTime;
        if (version != this.version) {
            return null;
        }

        order.id = this.id;
        order.userId = this.userId;
        order.sequenceId = this.sequenceId;
        order.price = this.price;
        order.direction = this.direction;
        order.quantity = this.quantity;
        order.createTime = this.createTime;
        return order;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OrderEntity) {
            return this.id.longValue() == ((OrderEntity) obj).id.longValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "OrderEntity [id=" + id + ", sequenceId=" + sequenceId + ", direction=" + direction + ", userId="
                + userId + ", status=" + status + ", price=" + price + ", createTime=" + createTime + ", updateTime="
                + updateTime + ", version=" + version + ", quantity=" + quantity + ", unfilledQuantity="
                + unfilledQuantity + "]";
    }

    /**
     * 按订单ID排序
     */
    @Override
    public int compareTo(OrderEntity o) {
        return Long.compare(this.id.longValue(), o.id.longValue());
    }
}
