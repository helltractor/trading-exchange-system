package com.helltractor.exchange.service;

import com.helltractor.exchange.ApiError;
import com.helltractor.exchange.ApiException;
import com.helltractor.exchange.support.LoggerSupport;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Trading engine API proxy service.
 */
@Component
public class TradingEngineApiProxyService extends LoggerSupport {
    
    @Value("#{exchangeConfiguration.apiEndpoints.tradingEngineApi}")
    private String tradingEngineInternalApiEndpoint;
    
    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(20, 60, TimeUnit.SECONDS))
            .retryOnConnectionFailure(false)
            .build();
    
    public String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(tradingEngineInternalApiEndpoint + url)
                .header("Accept", "*/*")
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                logger.error("Internal api failed with code {}: {}", Integer.valueOf(response.code()), url);
                throw new ApiException(ApiError.OPERATION_TIMEOUT, null, "operation timeout.");
            }
            try (ResponseBody body = response.body()) {
                String json = body.string();
                if (json == null || json.isEmpty()) {
                    logger.error("Internal api failed with code 200 but empty response: {}", json);
                    throw new ApiException(ApiError.INTERNAL_SERVER_ERROR, null, "response is empty.");
                }
                return json;
            }
        }
    }
}
