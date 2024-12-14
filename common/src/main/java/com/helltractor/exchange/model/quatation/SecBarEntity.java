package com.helltractor.exchange.model.quatation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.helltractor.exchange.model.support.AbstractBarEntity;

/**
 * Store bars of second.
 */
@Entity
@Table(name = "sec_bars")
public class SecBarEntity extends AbstractBarEntity {
}
