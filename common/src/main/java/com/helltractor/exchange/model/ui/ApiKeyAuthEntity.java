package com.helltractor.exchange.model.ui;

import com.helltractor.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "api_key_auths")
public class ApiKeyAuthEntity implements EntitySupport {

    /**
     * Primary key: generated api key.
     */
    @Id
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public String apiKey;

    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public String apiSecret;

    /**
     * Reference to user id.
     */
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * API key expires time in milliseconds.
     */
    @Column(nullable = false, updatable = false)
    public long expiresAt;

    @Override
    public String toString() {
        return "ApiKeyAuthEntity [apiKey=" + apiKey + ", apiSecret=******, userId=" + userId + ", expiresAt="
                + expiresAt + "]";
    }
}
