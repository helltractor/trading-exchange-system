package com.warp.exchange.entity.quatation;

import com.warp.exchange.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 日线数据
 */
@Entity
@Table(name = "day_bar")
public class DayBarEntity extends AbstractBarEntity {

}
