package com.helltractor.exchange.model.quatation;

import java.math.BigDecimal;

import com.helltractor.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ticks", uniqueConstraints = @UniqueConstraint(name = "UNI_T_M", columnNames = {"takerOrderId",
    "makerOrderId"}), indexes = @Index(name = "IDX_CAT", columnList = "createTime"))
public class TickEntity implements EntitySupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    public long id;

    @Column(nullable = false, updatable = false)
    public long sequenceId;

    @Column(nullable = false, updatable = false)
    public Long takerOrderId;

    @Column(nullable = false, updatable = false)
    public Long makerOrderId;

    /**
     * Bit for taker direction: 1=LONG, 0=SHORT.
     */
    @Column(nullable = false, updatable = false)
    public boolean takerDirection;

    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal price;

    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal quantity;

    /**
     * Created time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long createTime;

    public String toJson() {
        return "[" + createTime + "," + (takerDirection ? 1 : 0) + "," + price + "," + quantity + "]";
    }
}
