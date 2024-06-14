package com.helltractor.exchange.entity.quatation;

import com.helltractor.exchange.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 秒线数据
 */
@Entity
@Table(name = "sec_bars")
public class SecBarEntity extends AbstractBarEntity {
}
