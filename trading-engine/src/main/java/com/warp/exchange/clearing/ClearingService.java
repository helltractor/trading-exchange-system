package com.warp.exchange.clearing;

import com.warp.exchange.asset.AssetService;
import com.warp.exchange.entity.trade.OrderEntity;
import com.warp.exchange.enums.AssetEnum;
import com.warp.exchange.enums.Transfer;
import com.warp.exchange.match.MatchDetailRecord;
import com.warp.exchange.match.MatchResult;
import com.warp.exchange.order.OrderService;
import com.warp.exchange.support.LoggerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ClearingService extends LoggerSupport {
    final AssetService assetService;
    final OrderService orderService;
    
    @Value("${exchange.fee-rate:0.0005}")
    BigDecimal feeRate;
    
    public ClearingService(@Autowired AssetService assetService, @Autowired OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }
    
    /**
     * 清算撮合结果
     *
     * @param result
     */
    public void clearMatchResult(MatchResult result) {
        OrderEntity taker = result.takerOrder;
        switch (taker.direction) {
            case BUY -> {
                for (MatchDetailRecord detail : result.matchDetails) {
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal matched = detail.quantity();
                    if (taker.price.compareTo(maker.price) > 0) {
                        // 实际买入价比报价低，部分USD退回账户:
                        BigDecimal unfreezeQuote = taker.price.subtract(maker.price).multiply(matched);
                        logger.debug("unfree extra unused quote {} back to taker user {}", unfreezeQuote, taker.userId);
                        assetService.unfreeze(taker.userId, AssetEnum.USD, unfreezeQuote);
                    }
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.USD, maker.price.multiply(matched));
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.BTC, matched);
                    // 删除完全成交的Maker
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.id);
                    }
                }
                // 删除完全成交的Taker
                if (taker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(taker.id);
                }
            }
            case SELL -> {
                for (MatchDetailRecord detail : result.matchDetails) {
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal matched = detail.quantity();
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.BTC, matched);
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.USD, maker.price.multiply(matched));
                    // 删除完全成交的Maker
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.id);
                    }
                }
                // 删除完全成交的Taker
                if (taker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(taker.id);
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction: " + taker.direction);
        }
    }
    
    /**
     * 清算取消订单
     *
     * @param order
     */
    public void clearCancelOrder(OrderEntity order) {
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
