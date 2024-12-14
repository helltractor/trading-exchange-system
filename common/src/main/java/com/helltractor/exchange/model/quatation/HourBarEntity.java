package com.helltractor.exchange.model.quatation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.helltractor.exchange.model.support.AbstractBarEntity;

/**
 * Store bars of hour.
 */
@Entity
@Table(name = "hour_bars")
public class HourBarEntity extends AbstractBarEntity {
}
