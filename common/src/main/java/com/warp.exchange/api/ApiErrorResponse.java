package com.warp.exchange.api;

import com.warp.exchange.enums.ApiError;

public record ApiErrorResponse(ApiError error, String data, String message) {
}
