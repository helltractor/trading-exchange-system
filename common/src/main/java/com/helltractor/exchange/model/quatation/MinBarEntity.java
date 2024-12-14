package com.helltractor.exchange.model.quatation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.helltractor.exchange.model.support.AbstractBarEntity;

/**
 * Store bars of minute.
 */
@Entity
@Table(name = "min_bars")
public class MinBarEntity extends AbstractBarEntity {
}
