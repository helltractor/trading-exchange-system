package com.warp.exchange.service;

import com.warp.exchange.entity.Order;
import com.warp.exchange.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderService {
    final AssetService assetService;

    final ConcurrentHashMap<Long, Order> activeOrders = new ConcurrentHashMap<>();  // 跟踪所有活动订单
    final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Order>> userOrders = new ConcurrentHashMap<>();    // 跟踪用户活动订单

    public OrderService(@Autowired AssetService assetService) {
        this.assetService = assetService;
    }

    public Order getOrder(Long orderId) {
        return this.activeOrders.get(orderId);
    }

    public ConcurrentHashMap<Long, Order> getActiveOrders() {
        return this.activeOrders;
    }

    public ConcurrentHashMap<Long, Order> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }

    /**
     * 创建订单
     *
     * @param sequenceId
     * @param timeStamp
     * @param orderId
     * @param userId
     * @param direction
     * @param price
     * @param quantity
     * @return
     */
    public Order createOrder(long sequenceId, long timeStamp, Long orderId, Long userId, DirectionEnum direction, BigDecimal price, BigDecimal quantity) {
        switch (direction) {
            case BUY -> {
                // 买入，需冻结USD：
                if (!assetService.tryFreeze(userId, AssetEnum.USD, price.multiply(quantity))) {
                    return null;
                }
            }
            case SELL -> {
                // 卖出，需冻结BTC：
                if (!assetService.tryFreeze(userId, AssetEnum.BTC, quantity)) {
                    return null;
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        }
        Order order = new Order();
        order.orderId = orderId;
        order.userId = userId;
        order.sequenceId = sequenceId;
        order.price = price;
        order.direction = direction;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.createTime = order.updateTime = timeStamp;

        this.activeOrders.put(sequenceId, order);
        ConcurrentHashMap<Long, Order> tmpUserOrders = this.userOrders.get(userId);
        if (tmpUserOrders == null) {
            tmpUserOrders = new ConcurrentHashMap<>();
            userOrders.put(userId, tmpUserOrders);
        }
        tmpUserOrders.put(sequenceId, order);
        return order;
    }

    /**
     * 删除订单
     *
     * @param orderId
     */
    public void removeOrder(long orderId) {
        Order removed = this.activeOrders.remove(orderId);
        if (removed == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        ConcurrentHashMap<Long, Order> tmpUserOrders = this.userOrders.get(removed.userId);
        if (tmpUserOrders == null) {
            throw new IllegalArgumentException("User removed not found: " + removed.userId);
        }
        if (tmpUserOrders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found in user orders: " + removed.userId);
        }
    }
}
