package com.helltractor.exchange.support;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.helltractor.exchange.ApiError;
import com.helltractor.exchange.ApiErrorResponse;
import com.helltractor.exchange.ApiException;

import jakarta.servlet.http.HttpServletResponse;

public class AbstractApiController extends LoggerSupport {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public ApiErrorResponse handleApiException(HttpServletResponse response, Exception e) {
        response.setContentType("application/json;charset=UTF-8");
        ApiException apiException = null;
        if (e instanceof ApiException) {
            apiException = (ApiException) e;
        } else {
            apiException = new ApiException(ApiError.INTERNAL_SERVER_ERROR, null, e.getMessage());
        }
        return apiException.error;
    }
}
