package com.helltractor.exchange.ctx;

import com.helltractor.exchange.ApiError;
import com.helltractor.exchange.ApiException;

/**
 * store user context in thread local
 */
public class UserContext implements AutoCloseable {
    
    private static final ThreadLocal<Long> THREAD_LOCAL_CTX = new ThreadLocal<>();
    
    public UserContext(Long userId) {
        THREAD_LOCAL_CTX.set(userId);
    }
    
    public static Long getRequiredUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_REQUIRED, null, "Need signin first.");
        }
        return userId;
    }
    
    public static Long getUserId() {
        return THREAD_LOCAL_CTX.get();
    }
    
    @Override
    public void close() {
        THREAD_LOCAL_CTX.remove();
    }
}
