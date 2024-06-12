package com.warp.exchange.entity.quatation;

import com.warp.exchange.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 秒线数据
 */
@Entity
@Table(name = "sec_bar")
public class SecBarEntity extends AbstractBarEntity {
}
