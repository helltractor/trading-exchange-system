package com.helltractor.exchange.model.ui;

import com.helltractor.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_auths")
public class PasswordAuthEntity implements EntitySupport {

    @Id
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * 随机字符串用于创建Hmac-SHA256.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public String random;

    /**
     * 存储HmacSHA256哈希 password = HmacSHA256(原始口令, key=random).
     */
    @Column(nullable = false, length = VAR_CHAR_100)
    public String password;
}
