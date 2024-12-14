package com.helltractor.exchange.model.quatation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.helltractor.exchange.model.support.AbstractBarEntity;

/**
 * Store bars of day.
 */
@Entity
@Table(name = "day_bars")
public class DayBarEntity extends AbstractBarEntity {
}
