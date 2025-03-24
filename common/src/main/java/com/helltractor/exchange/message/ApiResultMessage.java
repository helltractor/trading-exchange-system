package com.helltractor.exchange.message;

import com.helltractor.exchange.ApiError;
import com.helltractor.exchange.ApiErrorResponse;
import com.helltractor.exchange.model.trade.OrderEntity;

/**
 * API result message.
 */
public class ApiResultMessage extends AbstractMessage {
    
    static final ApiErrorResponse CREATE_ORDER_FAILED = new ApiErrorResponse(ApiError.NO_ENOUGH_ASSET, null,
            "No enough available assets");
    
    static final ApiErrorResponse CANCEL_ORDER_FAILED = new ApiErrorResponse(ApiError.ORDER_NOT_FOUND, null,
            "Order not found..");
    
    public ApiErrorResponse error;
    
    public Object result;
    
    public static ApiResultMessage createOrderFailed(String refId, long ts) {
        ApiResultMessage msg = new ApiResultMessage();
        msg.error = CREATE_ORDER_FAILED;
        msg.refId = refId;
        msg.createTime = ts;
        return msg;
    }
    
    public static ApiResultMessage cancelOrderFailed(String refId, long ts) {
        ApiResultMessage msg = new ApiResultMessage();
        msg.error = CANCEL_ORDER_FAILED;
        msg.refId = refId;
        msg.createTime = ts;
        return msg;
    }
    
    public static ApiResultMessage orderSuccess(String refId, OrderEntity order, long ts) {
        ApiResultMessage msg = new ApiResultMessage();
        msg.result = order;
        msg.refId = refId;
        msg.createTime = ts;
        return msg;
    }
}
