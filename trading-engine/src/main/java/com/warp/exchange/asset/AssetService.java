package com.warp.exchange.asset;

import com.warp.exchange.enums.AssetEnum;
import com.warp.exchange.enums.Transfer;
import com.warp.exchange.support.LoggerSupport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AssetService extends LoggerSupport {

    // UserId -> Map(AssetEnum -> Assets[available/frozen])
    final ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> userAssets = new ConcurrentHashMap<>();

    /**
     * 获取用户资产
     *
     * @param userId
     * @param assetId
     * @return
     */
    public Asset getAsset(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> assets = userAssets.get(userId);
        if (assets == null) {
            return null;
        }
        return assets.get(assetId);
    }

    /**
     * 获取用户所有资产
     *
     * @param userId
     * @return
     */
    public Map<AssetEnum, Asset> getAssets(Long userId) {
        Map<AssetEnum, Asset> assets = userAssets.get(userId);
        if (assets == null) {
            return Map.of();
        }
        return assets;
    }

    /**
     * 获取所有用户资产
     *
     * @return
     */
    public ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> getUserAssets() {
        return this.userAssets;
    }

    /**
     * 初始化资产
     *
     * @param userId
     * @param assetId
     * @return
     */
    public Asset initAssets(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> assets = userAssets.get(userId);
        if (assets == null) {
            assets = new ConcurrentHashMap<>();
            userAssets.put(userId, assets);
        }
        Asset zeroAsset = new Asset();
        assets.put(assetId, zeroAsset);
        return zeroAsset;
    }

    /**
     * 转账
     *
     * @param type
     * @param fromUserId
     * @param toUserId
     * @param assetId
     * @param amount
     * @param checkBalance
     * @return
     */
    public boolean tryTransfer(Transfer type, Long fromUserId, Long toUserId, AssetEnum assetId,
                               BigDecimal amount, boolean checkBalance) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Asset fromAsset = getAsset(fromUserId, assetId);
        if (fromAsset == null) {
            fromAsset = initAssets(fromUserId, assetId);
        }

        Asset toAsset = getAsset(toUserId, assetId);
        if (toAsset == null) {
            toAsset = initAssets(toUserId, assetId);
        }
        return switch (type) {
            case AVAILABLE_TO_AVAILABLE -> {
                if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            case FROZEN_TO_AVAILABLE -> {
                if (checkBalance && fromAsset.frozen.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.frozen = fromAsset.frozen.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            case AVAILABLE_TO_FROZEN -> {
                if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.frozen = toAsset.frozen.add(amount);
                yield true;
            }
            default -> {
                throw new IllegalArgumentException("Invalid transfer type: " + type);
            }
        };
    }

    /**
     * 检查是否可以转账
     *
     * @param type
     * @param fromUserId
     * @param toUserId
     * @param assetId
     * @param amount
     */
    public void transfer(Transfer type, Long fromUserId, Long toUserId, AssetEnum assetId, BigDecimal amount) {
        if (!tryTransfer(type, fromUserId, toUserId, assetId, amount, true)) {
            throw new RuntimeException("Transfer failed for " + type + ", from user " + fromUserId + " to user " + toUserId
                    + ", asset = " + assetId + ", amount = " + amount);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("transfer asset {}, from {} => {}, amount {}", assetId, fromUserId, toUserId, amount);
        }
    }

    /**
     * 冻结资产
     *
     * @param userId
     * @param assetId
     * @param amount
     * @return
     */
    public boolean tryFreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        boolean ok = tryTransfer(Transfer.AVAILABLE_TO_FROZEN, userId, userId, assetId, amount, true);
        if (ok && logger.isDebugEnabled()) {
            logger.debug("freezed user {}, asset {}, amount {}", userId, assetId, amount);
        }
        return ok;
    }

    /**
     * 解结资产
     *
     * @param userId
     * @param assetId
     * @param amount
     */
    public void unfreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        if (!tryTransfer(Transfer.FROZEN_TO_AVAILABLE, userId, userId, assetId, amount, true)) {
            throw new RuntimeException(
                    "Unfreeze failed for user " + userId + ", asset = " + assetId + ", amount = " + amount);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("unfreezed user {}, asset {}, amount {}", userId, assetId, amount);
        }
    }
    
    public void debug() {
        System.out.println("---------- assets ----------");
        List<Long> userIds = new ArrayList<>(userAssets.keySet());
        Collections.sort(userIds);
        for (Long userId : userIds) {
            System.out.println("  user " + userId + " ----------");
            Map<AssetEnum, Asset> assets = userAssets.get(userId);
            List<AssetEnum> assetIds = new ArrayList<>(assets.keySet());
            Collections.sort(assetIds);
            for (AssetEnum assetId : assetIds) {
                System.out.println("    " + assetId + ": " + assets.get(assetId));
            }
        }
        System.out.println("---------- // assets ----------");
    }
}
