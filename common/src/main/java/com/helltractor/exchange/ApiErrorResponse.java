package com.helltractor.exchange;

public record ApiErrorResponse(ApiError error, String data, String message) {

}
