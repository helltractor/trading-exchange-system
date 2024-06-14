package com.helltractor.exchange.api;

import com.helltractor.exchange.enums.ApiError;

public record ApiErrorResponse(ApiError error, String data, String message) {
}
