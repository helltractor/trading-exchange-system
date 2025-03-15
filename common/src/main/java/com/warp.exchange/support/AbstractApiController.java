package com.warp.exchange.support;

import com.warp.exchange.api.ApiErrorResponse;
import com.warp.exchange.api.ApiException;
import com.warp.exchange.enums.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

public class AbstractApiController extends LoggerSupport {
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public ApiErrorResponse handleApiException(HttpServletResponse response, Exception e) throws Exception {
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
