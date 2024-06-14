package com.helltractor.exchange.entity.quatation;

import com.helltractor.exchange.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 小时线数据
 */
@Entity
@Table(name = "hours_bars")
public class HourBarEntity extends AbstractBarEntity {

}