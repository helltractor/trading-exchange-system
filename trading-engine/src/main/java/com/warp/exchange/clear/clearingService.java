package com.warp.exchange.clear;

import com.warp.exchange.asset.AssetService;
import com.warp.exchange.enums.AssetEnum;
import com.warp.exchange.enums.TransferEnum;
import com.warp.exchange.match.MatchDetailRecord;
import com.warp.exchange.match.MatchResult;
import com.warp.exchange.order.Order;
import com.warp.exchange.order.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;

public class clearingService {
    final AssetService assetService;
    final OrderService orderService;

    @Value("${exchange.fee-rate:0.0005}")
    BigDecimal feeRate;

    public clearingService(@Autowired AssetService assetService, @Autowired OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    /**
     * 清算撮合结果
     *
     * @param result
     */
    public void clearMatchResult(MatchResult result) {
        Order takerOrder = result.takerOrder;
        switch (takerOrder.direction) {
            case BUY -> {
                for (MatchDetailRecord detail : result.matchDetails) {
                    Order makerOrder = detail.makerOrder;
                    BigDecimal matched = detail.quantity;
                    if (takerOrder.price.compareTo(makerOrder.price) > 0) {
                        // 实际买入价比报价低，部分USD退回账户:
                        BigDecimal unfreezeQuote = takerOrder.price.subtract(makerOrder.price).multiply(matched);
                        assetService.unfreeze(takerOrder.userId, AssetEnum.USD, unfreezeQuote);
                    }
                    assetService.transfer(TransferEnum.FROZEN_TO_AVAILABLE, makerOrder.userId, takerOrder.userId, AssetEnum.USD, makerOrder.price.multiply(matched));
                    assetService.transfer(TransferEnum.FROZEN_TO_AVAILABLE, takerOrder.userId, makerOrder.userId, AssetEnum.BTC, matched);
                    // 删除完全成交的Maker
                    if (makerOrder.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(makerOrder.orderId);
                    }
                }
                // 删除完全成交的Taker
                if (takerOrder.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(takerOrder.orderId);
                }
            }
            case SELL -> {
                for (MatchDetailRecord detail : result.matchDetails) {
                    Order makerOrder = detail.makerOrder;
                    BigDecimal matched = detail.quantity;
                    assetService.transfer(TransferEnum.FROZEN_TO_AVAILABLE, takerOrder.userId, makerOrder.userId, AssetEnum.BTC, matched);
                    assetService.transfer(TransferEnum.FROZEN_TO_AVAILABLE, makerOrder.userId, takerOrder.userId, AssetEnum.USD, makerOrder.price.multiply(matched));
                    // 删除完全成交的Maker
                    if (makerOrder.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(makerOrder.orderId);
                    }
                }
                // 删除完全成交的Taker
                if (takerOrder.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(takerOrder.orderId);
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction: " + takerOrder.direction);
        }
    }

    /**
     * 清算取消订单
     *
     * @param order
     */
    public void clearCancelOrder(Order order) {
        switch (order.direction) {
            case BUY -> {
                // 解冻USD = 价格 * 未成交数量
                assetService.unfreeze(order.userId, AssetEnum.USD, order.price.multiply(order.unfilledQuantity));
            }
            case SELL -> {
                // 解冻BTC = 未成交数量
                assetService.unfreeze(order.userId, AssetEnum.BTC, order.unfilledQuantity);
            }
            default -> throw new IllegalArgumentException("Invalid direction: " + order.direction);
        }
    }
}
