package com.helltractor.exchange.model.ui;

import com.helltractor.exchange.enums.UserType;
import com.helltractor.exchange.model.support.EntitySupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity implements EntitySupport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    public Long id;
    
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public UserType type;
    
    /**
     * Create time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long createTime;
    
    @Override
    public String toString() {
        return "UserEntity [id=" + id + ", type=" + type + ", createTime=" + createTime + "]";
    }
}
