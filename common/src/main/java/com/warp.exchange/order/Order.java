package com.warp.exchange.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.warp.exchange.enums.*;
import com.warp.exchange.support.EntitySupport;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 订单实体
 */
@Entity
@Table(name = "orders")
public class Order implements EntitySupport, Comparable<Order> {

    /**
     * 主键，订单ID
     */
    @Id
    @Column(nullable = false, updatable = false)
    public Long orderId;

    /**
     * 用户ID
     */
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * 交易对ID
     */
    @Column(nullable = false, updatable = false)
    public Long symbolId;

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
    public DirectionEnum direction;

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
    @Column(nullable = false)
    public long updateTime; // 更新时间

    private int version;

    @Transient
    @JsonIgnore
    public int getVersion() {
        return version;
    }

    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus status, long timeStamp) {
        this.status = status;
        this.unfilledQuantity = unfilledQuantity;
        this.updateTime = timeStamp;
        this.version++;
    }

    @Nullable
    public Order copy(){
        Order order = new Order();
        int version = this.version;
        order.status = this.status;
        order.unfilledQuantity = this.unfilledQuantity;
        order.updateTime = this.updateTime;
        if (version != this.version) {
            return null;
        }

        order.orderId = this.orderId;
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
        if (obj instanceof Order) {
            return this.orderId.longValue() == ((Order) obj).orderId.longValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.orderId.hashCode();
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", sequenceId=" + sequenceId +
                ", price=" + price +
                ", direction=" + direction +
                ", status=" + status +
                ", quantity=" + quantity +
                ", unfilledQuantity=" + unfilledQuantity +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", version=" + version +
                '}';
    }

    /**
     * 按订单ID排序
     *
     * @param o the object to be compared.
     * @return
     */
    @Override
    public int compareTo(Order o) {
        return Long.compare(this.orderId.longValue(), o.orderId.longValue());
    }
}
