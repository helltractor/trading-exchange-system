package com.helltractor.exchange.bean;

import com.helltractor.exchange.ApiError;
import com.helltractor.exchange.ApiException;
import com.helltractor.exchange.enums.AssetEnum;
import com.helltractor.exchange.util.IdUtil;

import java.math.BigDecimal;

public class TransferRequestBean implements ValidatableBean {
    
    public String transferId;
    public AssetEnum asset;
    public Long fromUserId;
    public Long toUserId;
    public BigDecimal amount;
    
    @Override
    public void validate() {
        if (!IdUtil.isValidStringId(transferId)) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "transferId", "Must specify a unique transferId.");
        }
        if (fromUserId == null || fromUserId.longValue() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "fromUserId", "Must specify fromUserId.");
        }
        if (toUserId == null || toUserId.longValue() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "toUserId", "Must specify toUserId.");
        }
        if (fromUserId.longValue() == toUserId.longValue()) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "toUserId", "Must be different with fromUserId.");
        }
        if (asset == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "assets", "Must specify assets.");
        }
        if (amount == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "amount", "Must specify amount.");
        }
        amount = amount.setScale(AssetEnum.SCALE);
        if (amount.signum() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "amount", "Must specify positive amount.");
        }
    }
}
