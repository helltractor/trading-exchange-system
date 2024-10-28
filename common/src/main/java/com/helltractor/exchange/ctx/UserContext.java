package com.helltractor.exchange.ctx;

import com.helltractor.exchange.api.ApiException;
import com.helltractor.exchange.enums.ApiError;

/**
 * 在线程本地中保存用户上下文
 */
public class UserContext implements AutoCloseable {
    
    private static final ThreadLocal<Long> THREAD_LOCAL_CTX = new ThreadLocal<>();
    
    public UserContext(Long userId) {
        THREAD_LOCAL_CTX.set(userId);
    }
    
    /**
     * 获取当前用户ID，如果没有用户，则引发异常
     */
    public static Long getRequiredUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_REQUIRED, null, "Need signin first.");
        }
        return userId;
    }
    
    /**
     * 获取当前用户ID，如果没有用户则返回null
     */
    public static Long getUserId() {
        return THREAD_LOCAL_CTX.get();
    }
    
    @Override
    public void close() {
        THREAD_LOCAL_CTX.remove();
    }
}