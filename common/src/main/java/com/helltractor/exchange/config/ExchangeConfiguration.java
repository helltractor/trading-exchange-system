package com.helltractor.exchange.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.ZoneId;

@Configuration
@ConfigurationProperties(prefix = "exchange.config")
public class ExchangeConfiguration {
    
    int orderBookDepth = 50;
    boolean debugMode = false;
    String timeZone = ZoneId.systemDefault().getId();
    String hmacKey;
    Duration sessionTimeout;
    ApiEndpoints apiEndpoints;
    
    @Bean
    public ZoneId createZoneId() {
        return ZoneId.of(this.timeZone);
    }
    
    public int getOrderBookDepth() {
        return orderBookDepth;
    }
    
    public void setOrderBookDepth(int orderBookDepth) {
        this.orderBookDepth = orderBookDepth;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    
    public String getTimeZone() {
        return timeZone;
    }
    
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone.isEmpty() ? ZoneId.systemDefault().getId() : timeZone;
    }
    
    public String getHmacKey() {
        return hmacKey;
    }
    
    public void setHmacKey(String hmacKey) {
        this.hmacKey = hmacKey;
    }
    
    public Duration getSessionTimeout() {
        return sessionTimeout;
    }
    
    public void setSessionTimeout(Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
    
    public ApiEndpoints getApiEndpoints() {
        return apiEndpoints;
    }
    
    public void setApiEndpoints(ApiEndpoints apiEndpoints) {
        this.apiEndpoints = apiEndpoints;
    }
    
    public static class ApiEndpoints {
        
        String tradingApi;
        String tradingEngineApi;
        
        public String getTradingApi() {
            return tradingApi;
        }
        
        public void setTradingApi(String tradingApi) {
            this.tradingApi = tradingApi;
        }
        
        public String getTradingEngineApi() {
            return tradingEngineApi;
        }
        
        public void setTradingEngineApi(String tradingEngineApi) {
            this.tradingEngineApi = tradingEngineApi;
        }
    }
}
