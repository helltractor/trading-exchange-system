package com.warp.exchange.entity;

import java.math.BigDecimal;

public class Asset {
    public BigDecimal available;
    public BigDecimal frozen;

    public Asset() {
        this(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public Asset(BigDecimal available, BigDecimal frozen) {
        this.available = available;
        this.frozen = frozen;
    }
}
