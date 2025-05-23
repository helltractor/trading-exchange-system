package com.helltractor.exchange.message.event;

import java.math.BigDecimal;

import com.helltractor.exchange.enums.AssetEnum;

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
                + toUserId + ", assets=" + asset + ", amount=" + amount + ", sufficient=" + sufficient + "]";
    }
}
