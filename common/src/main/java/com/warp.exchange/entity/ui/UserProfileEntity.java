package com.warp.exchange.entity.ui;

import com.warp.exchange.support.EntitySupport;
import jakarta.persistence.*;

@Entity
@Table(name = "user_profiles", uniqueConstraints = {@UniqueConstraint(name = "UNI_EMAIL", columnNames = {"email"})})
public class UserProfileEntity implements EntitySupport {
    
    /**
     * 关联至用户ID.
     */
    @Id
    @Column(nullable = false, updatable = false)
    public Long userId;
    
    /**
     * 登录Email
     */
    @Column(nullable = false, updatable = false, length = VAR_CHAR_100)
    public String email;
    
    @Column(nullable = false, length = VAR_CHAR_100)
    public String name;
    
    @Column(nullable = false, updatable = false)
    public long createTime;
    
    @Column(nullable = false)
    public long updateTime;
    
    @Override
    public String toString() {
        return "UserProfileEntity [userId=" + userId + ", email=" + email + ", name=" + name + ", createTime="
                + createTime + ", updateTime=" + updateTime + "]";
    }
}