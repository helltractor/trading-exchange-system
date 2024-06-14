package com.helltractor.exchange.entity.quatation;

import com.helltractor.exchange.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 日线数据
 */
@Entity
@Table(name = "day_bars")
public class DayBarEntity extends AbstractBarEntity {
}