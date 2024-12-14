package com.helltractor.exchange.model.quatation;

import com.helltractor.exchange.model.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Store bars of second.
 */
@Entity
@Table(name = "sec_bars")
public class SecBarEntity extends AbstractBarEntity {
}
