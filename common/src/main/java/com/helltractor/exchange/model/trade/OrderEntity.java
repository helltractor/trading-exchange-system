package com.helltractor.exchange.model.trade;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.lang.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.helltractor.exchange.enums.Direction;
import com.helltractor.exchange.enums.OrderStatus;
import com.helltractor.exchange.model.support.EntitySupport;

/**
 * Order entity.
 */
@Entity
@Table(name = "orders")
public class OrderEntity implements EntitySupport, Comparable<OrderEntity> {

    /**
     * Primary key: assigned order id.
     */
    @Id
    @Column(nullable = false, updatable = false)
    public Long id;

    /**
     * event id (a.k.a sequenceId) that create this order. ASC only.
     */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * Order direction.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public Direction direction;

    /**
     * User id of this order.
     */
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * Order status.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public OrderStatus status;

    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus status, long updateTime) {
        this.version++;
        this.unfilledQuantity = unfilledQuantity;
        this.status = status;
        this.updateTime = updateTime;
        this.version++;
    }

    /**
     * The limit-order price. MUST NOT change after insert.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal price;

    /**
     * Created time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long createTime;

    /**
     * Updated time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long updateTime;

    private int version;

    @Transient
    @JsonIgnore
    public int getVersion() {
        return this.version;
    }

    /**
     * The order quantity. MUST NOT change after insert.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal quantity;

    /**
     * How much unfilled during match.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal unfilledQuantity;

    @Nullable
    public OrderEntity copy() {
        OrderEntity entity = new OrderEntity();
        int ver = this.version;
        entity.status = this.status;
        entity.unfilledQuantity = this.unfilledQuantity;
        entity.updateTime = this.updateTime;
        if (ver != this.version) {
            return null;
        }

        entity.createTime = this.createTime;
        entity.direction = this.direction;
        entity.id = this.id;
        entity.price = this.price;
        entity.quantity = this.quantity;
        entity.sequenceId = this.sequenceId;
        entity.userId = this.userId;
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof OrderEntity) {
            OrderEntity e = (OrderEntity) o;
            return this.id.longValue() == e.id.longValue();
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
     * Sort by OrderId.
     */
    @Override
    public int compareTo(OrderEntity o) {
        return Long.compare(this.id.longValue(), o.id.longValue());
    }
}
