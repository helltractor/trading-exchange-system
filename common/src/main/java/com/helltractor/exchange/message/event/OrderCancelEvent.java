package com.helltractor.exchange.message.event;

public class OrderCancelEvent extends AbstractEvent {
    
    public Long userId;
    
    public Long refOrderId;
    
    @Override
    public String toString() {
        return "OrderCancelEvent [sequenceId=" + sequenceId + ", previousId=" + previousId + ", uniqueId=" + uniqueId
                + ", refId=" + refId + ", createTime=" + createTime + ", userId=" + userId + ", refOrderId=" + refOrderId
                + "]";
    }
}