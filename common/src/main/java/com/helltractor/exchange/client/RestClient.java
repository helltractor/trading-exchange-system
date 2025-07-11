package com.helltractor.exchange.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helltractor.exchange.ApiError;
import com.helltractor.exchange.ApiErrorResponse;
import com.helltractor.exchange.ApiException;

import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * OkHttp client for accessing exchange REST APIs.
 */
public class RestClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final ApiException ERROR_UNKNOWN = new ApiException(ApiError.INTERNAL_SERVER_ERROR, "api", "Api failed without error code.");

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String endpoint;

    private final String host;

    private final ObjectMapper objectMapper;

    OkHttpClient okHttpClient;

    RestClient(String endpoint, String host, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.endpoint = endpoint;
        this.host = host;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
    }

    public <T> T get(Class<T> clazz, String path, String authHeader, Map<String, String> query) {
        Objects.requireNonNull(clazz);
        return request(clazz, null, "GET", path, authHeader, query, null);
    }

    public <T> T get(TypeReference<T> ref, String path, String authHeader, Map<String, String> query) {
        Objects.requireNonNull(ref);
        return request(null, ref, "GET", path, authHeader, query, null);
    }

    public <T> T post(Class<T> clazz, String path, String authHeader, Object body) {
        Objects.requireNonNull(clazz);
        return request(clazz, null, "POST", path, authHeader, null, body);
    }

    public <T> T post(TypeReference<T> ref, String path, String authHeader, Object body) {
        Objects.requireNonNull(ref);
        return request(null, ref, "POST", path, authHeader, null, body);
    }

    <T> T request(Class<T> clazz, TypeReference<T> ref, String method, String path, String authHeader,
            Map<String, String> query, Object body) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        // query
        String queryString = null;
        if (query != null) {
            List<String> paramList = new ArrayList<>();
            for (Map.Entry<String, String> entry : query.entrySet()) {
                paramList.add(entry.getKey() + "=" + entry.getValue());
            }
            queryString = String.join("&", paramList);
        }
        StringBuilder urlBuilder = new StringBuilder(64).append(this.endpoint).append(path);
        if (queryString != null) {
            urlBuilder.append('?').append(queryString);
        }
        final String url = urlBuilder.toString();

        // json body
        String jsonBody;
        try {
            jsonBody = body == null ? ""
                    : (body instanceof String ? (String) body : objectMapper.writeValueAsString(body));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (authHeader != null) {
            requestBuilder.addHeader("Authorization", authHeader);
        }
        if ("POST".equals(method)) {
            requestBuilder.post(RequestBody.create(jsonBody, JSON));
        }

        Request request = requestBuilder.build();
        try {
            return execute(clazz, ref, request);
        } catch (IOException e) {
            logger.warn("IOException", e);
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    <T> T execute(Class<T> clazz, TypeReference<T> ref, Request request) throws IOException {
        logger.info("request: {}...", request.url().url());
        try (Response response = this.okHttpClient.newCall(request).execute()) {
            switch (response.code()) {
                case 200 -> {
                    try (ResponseBody body = response.body()) {
                        String json = body.string();
                        if ("null".equals(json)) {
                            return null;
                        }
                        if (clazz == null) {
                            return objectMapper.readValue(json, ref);
                        }
                        if (clazz == String.class) {
                            return (T) json;
                        }
                        return objectMapper.readValue(json, clazz);
                    }
                }
                case 400 -> {
                    try (ResponseBody body = response.body()) {
                        String bodyString = body.string();
                        logger.warn("response 400. error: {}", bodyString);
                        ApiErrorResponse err = objectMapper.readValue(bodyString, ApiErrorResponse.class);
                        if (err == null || err.error() == null) {
                            throw ERROR_UNKNOWN;
                        }
                        throw new ApiException(err.error(), err.data(), err.message());
                    }
                }
                default -> {
                    throw new ApiException(ApiError.INTERNAL_SERVER_ERROR, null, "Http error " + response.code());
                }
            }
        }
    }

    public static class Builder {

        final Logger logger = LoggerFactory.getLogger(getClass());

        private final String scheme;

        private final String host;

        private final int port;

        private int connectTimeout = 3;
        private int readTimeout = 3;
        private int keepAlive = 30;

        /**
         * Create builder with api endpoint. e.g. "http://localhost:8080". NOTE:
         * do not append any PATH.
         *
         * @param apiEndpoint The api endpoint.
         */
        public Builder(String apiEndpoint) {
            logger.info("build RestClient from {}...", apiEndpoint);
            try {
                URI uri = new URI(apiEndpoint);
                if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) {
                    throw new IllegalArgumentException("Invalid API endpoint: " + apiEndpoint);
                }
                if (uri.getPath() != null && !uri.getPath().isEmpty()) {
                    throw new IllegalArgumentException("Invalid API endpoint: " + apiEndpoint);
                }
                this.scheme = uri.getScheme();
                this.host = uri.getHost().toLowerCase();
                this.port = uri.getPort();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid API endpoint: " + apiEndpoint, e);
            }
        }

        public Builder connectTimeout(int connectTimeoutInSeconds) {
            this.connectTimeout = connectTimeoutInSeconds;
            return this;
        }

        public Builder readTimeout(int readTimeoutInSeconds) {
            this.readTimeout = readTimeoutInSeconds;
            return this;
        }

        public Builder keepAlive(int keepAliveInSeconds) {
            this.keepAlive = keepAliveInSeconds;
            return this;
        }

        public RestClient build(ObjectMapper objectMapper) {
            OkHttpClient client = new OkHttpClient.Builder()
                    // set connect timeout
                    .connectTimeout(this.connectTimeout, TimeUnit.SECONDS)
                    // set read timeout
                    .readTimeout(this.readTimeout, TimeUnit.SECONDS)
                    // set connection pool
                    .connectionPool(new ConnectionPool(0, this.keepAlive, TimeUnit.SECONDS))
                    // do not retry
                    .retryOnConnectionFailure(false).build();
            String endpoint = this.scheme + "://" + this.host;
            if (this.port != (-1)) {
                endpoint = endpoint + ":" + this.port;
            }
            return new RestClient(endpoint, this.host, objectMapper, client);
        }
    }
}
