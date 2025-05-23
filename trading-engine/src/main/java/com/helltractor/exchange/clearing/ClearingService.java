package com.helltractor.exchange.clearing;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.helltractor.exchange.assets.AssetService;
import com.helltractor.exchange.assets.Transfer;
import com.helltractor.exchange.enums.AssetEnum;
import com.helltractor.exchange.match.MatchDetailRecord;
import com.helltractor.exchange.match.MatchResult;
import com.helltractor.exchange.model.trade.OrderEntity;
import com.helltractor.exchange.order.OrderService;
import com.helltractor.exchange.support.LoggerSupport;

@Component
public class ClearingService extends LoggerSupport {

    private final AssetService assetService;

    private final OrderService orderService;

    @Value("${exchange.fee-rate:0.0005}")
    private BigDecimal feeRate;

    public ClearingService(@Autowired AssetService assetService, @Autowired OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    public void clearMatchResult(MatchResult result) {
        OrderEntity taker = result.takerOrder;
        switch (taker.direction) {
            case BUY -> {
                // 买入时，按Maker的价格成交
                for (MatchDetailRecord detail : result.matchDetails) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "clear buy matched detail: price = {}, quantity = {}, takerOrderId = {}, makerOrderId = {}, takerUserId = {}, makerUserId = {}",
                                detail.price(), detail.quantity(), detail.takerOrder().id, detail.makerOrder().id,
                                detail.takerOrder().userId, detail.makerOrder().userId);
                    }
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal matched = detail.quantity();
                    if (taker.price.compareTo(maker.price) > 0) {
                        // 实际买入价比报价低，部分USD退回账户
                        BigDecimal unfreezeQuote = taker.price.subtract(maker.price).multiply(matched);
                        if (logger.isDebugEnabled()) {
                            logger.debug("unfree extra unused quote {} back to taker user {}", unfreezeQuote, taker.userId);
                        }
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
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "clear sell matched detail: price = {}, quantity = {}, takerOrderId = {}, makerOrderId = {}, takerUserId = {}, makerUserId = {}",
                                detail.price(), detail.quantity(), detail.takerOrder().id, detail.makerOrder().id,
                                detail.takerOrder().userId, detail.makerOrder().userId);
                    }
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
            default -> {
                throw new IllegalArgumentException("Invalid direction: " + taker.direction);
            }
        }
    }

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
            default -> {
                throw new IllegalArgumentException("Invalid direction: " + order.direction);
            }
        }
        orderService.removeOrder(order.id);
    }
}
