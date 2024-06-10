package com.warp.exchange.message.event;

import com.warp.exchange.enums.AssetEnum;

import java.math.BigDecimal;

/**
 * Transfer between users.
 */
public class TransferEvent extends AbstractEvent {
    
    public Long fromUserId;
    public Long toUserId;
    public AssetEnum asset;
    public BigDecimal amount;
    public boolean sufficient;
    
    @Override
    public String toString() {
        return "TransferEvent [sequenceId=" + sequenceId + ", previousId=" + previousId + ", uniqueId=" + uniqueId
                + ", refId=" + refId + ", createTime=" + createTime + ", fromUserId=" + fromUserId + ", toUserId="
                + toUserId + ", asset=" + asset + ", amount=" + amount + ", sufficient=" + sufficient + "]";
    }
}