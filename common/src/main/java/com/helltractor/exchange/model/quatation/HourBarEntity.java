package com.helltractor.exchange.model.quatation;

import com.helltractor.exchange.model.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Store bars of hour.
 */
@Entity
@Table(name = "hour_bars")
public class HourBarEntity extends AbstractBarEntity {
}
