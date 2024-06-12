package com.warp.exchange.entity.quatation;

import com.warp.exchange.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 分钟线数据
 */
@Entity
@Table(name = "min_bar")
public class MinBarEntity extends AbstractBarEntity {
}
