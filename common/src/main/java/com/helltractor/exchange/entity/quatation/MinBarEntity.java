package com.helltractor.exchange.entity.quatation;

import com.helltractor.exchange.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 分钟线数据
 */
@Entity
@Table(name = "min_bars")
public class MinBarEntity extends AbstractBarEntity {
}
