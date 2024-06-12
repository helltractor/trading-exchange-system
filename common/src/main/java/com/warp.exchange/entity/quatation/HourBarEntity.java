package com.warp.exchange.entity.quatation;

import com.warp.exchange.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 小时线数据
 */
@Entity
@Table(name = "hours_bar")
public class HourBarEntity extends AbstractBarEntity {

}