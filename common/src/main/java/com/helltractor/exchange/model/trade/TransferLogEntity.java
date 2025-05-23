package com.helltractor.exchange.model.trade;

import java.math.BigDecimal;

import com.helltractor.exchange.enums.AssetEnum;
import com.helltractor.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * User transfer log entity.
 */
@Entity
@Table(name = "transfer_logs")
public class TransferLogEntity implements EntitySupport {

    @Id
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public String transferId;

    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public AssetEnum asset;

    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal amount;

    @Column(nullable = false, updatable = false)
    public Long userId;

    @Column(nullable = false, updatable = false)
    public long createTime;

    @Column(nullable = false, length = VAR_ENUM)
    public String type;

    @Column(nullable = false, length = VAR_ENUM)
    public String status;
}
