package com.helltractor.exchange.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.helltractor.exchange.assets.AssetService;
import com.helltractor.exchange.enums.AssetEnum;
import com.helltractor.exchange.enums.Direction;
import com.helltractor.exchange.model.trade.OrderEntity;

@Component
public class OrderService {

    private final AssetService assetService;

    // active orders, orderId -> order
    private final ConcurrentMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();

    // all user orders, userId -> orderId -> order
    private final ConcurrentMap<Long, ConcurrentHashMap<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();

    public OrderService(@Autowired AssetService assetService) {
        this.assetService = assetService;
    }

    public OrderEntity getOrder(Long orderId) {
        return this.activeOrders.get(orderId);
    }

    public ConcurrentMap<Long, OrderEntity> getActiveOrders() {
        return this.activeOrders;
    }

    public ConcurrentMap<Long, OrderEntity> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }

    public OrderEntity createOrder(long sequenceId, long timeStamp, Long orderId, Long userId, Direction direction, BigDecimal price, BigDecimal quantity) {
        switch (direction) {
            case BUY -> {
                // buy, need freeze USD
                if (!assetService.tryFreeze(userId, AssetEnum.USD, price.multiply(quantity))) {
                    return null;
                }
            }
            case SELL -> {
                // sell, need freeze BTC
                if (!assetService.tryFreeze(userId, AssetEnum.BTC, quantity)) {
                    return null;
                }
            }
            default -> {
                throw new IllegalArgumentException("Invalid direction.");
            }
        }
        OrderEntity order = new OrderEntity();
        order.id = orderId;
        order.userId = userId;
        order.sequenceId = sequenceId;
        order.price = price;
        order.direction = direction;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.createTime = order.updateTime = timeStamp;
        // add to active orders
        this.activeOrders.put(order.id, order);
        // add to user orders
        this.userOrders.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(order.id, order);
        return order;
    }

    public void removeOrder(long orderId) {
        // remove from active orders
        OrderEntity removed = this.activeOrders.remove(orderId);
        if (removed == null) {
            throw new IllegalArgumentException("OrderEntity not found by orderId in active orders: " + orderId);
        }
        // remove from user orders
        ConcurrentHashMap<Long, OrderEntity> tmpUserOrders = this.userOrders.get(removed.userId);
        if (tmpUserOrders == null) {
            throw new IllegalArgumentException("User orders not found by userId: " + removed.userId);
        }
        if (tmpUserOrders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found by orderId in user orders: " + orderId);
        }
    }

    public void debug() {
        System.out.println("---------- orders ----------");
        List<OrderEntity> orders = new ArrayList<>(this.activeOrders.values());
        Collections.sort(orders);
        for (OrderEntity order : orders) {
            System.out.println("  " + order.id + " " + order.direction + " price: " + order.price + " unfilled: "
                    + order.unfilledQuantity + " quantity: " + order.quantity + " sequenceId: " + order.sequenceId
                    + " userId: " + order.userId);
        }
        System.out.println("---------- // orders ----------");
    }
}
