package com.warp.exchange.match;

import com.warp.exchange.order.Order;
import com.warp.exchange.order.OrderBook;
import com.warp.exchange.enums.DirectionEnum;
import com.warp.exchange.enums.OrderStatus;

import java.math.BigDecimal;

/**
 * 订单撮合引擎
 */
public class MatchEngine {
    public final OrderBook buyBook = new OrderBook(DirectionEnum.BUY);
    public final OrderBook sellBook = new OrderBook(DirectionEnum.SELL);
    public BigDecimal marketPrice = BigDecimal.ZERO;
    private long sequenceId;

    public MatchResult processOrder(long sequenceId, Order order) {
        switch (order.direction) {
            case BUY -> {
                return processOrder(
                        sequenceId,
                        order,
                        this.sellBook,
                        this.buyBook
                );
            }
            case SELL -> {
                return processOrder(
                        sequenceId,
                        order,
                        this.buyBook,
                        this.sellBook
                );
            }
            default -> {
                throw new IllegalArgumentException("Invalid direction: " + order.direction);
            }
        }
    }

    /**
     * 处理订单
     *
     * @param sequenceId
     * @param takerOrder  输入订单
     * @param makerBook   尝试匹配成交的OrderBook
     * @param anotherBook 未能完全成交后挂单的OrderBook
     * @return 成交结果
     */
    private MatchResult processOrder(long sequenceId, Order takerOrder, OrderBook makerBook, OrderBook anotherBook) {
        this.sequenceId = sequenceId;
        long timeStamp = takerOrder.createTime;
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal takerUnfilledQuantity = takerOrder.quantity;
        for (;;) {
            Order makerOrder = makerBook.getFirst();
            if (makerOrder == null) {
                // 没有匹配的订单
                break;
            }
            if (takerOrder.direction == DirectionEnum.BUY && makerOrder.price.compareTo(takerOrder.price) > 0) {
                // 买单价格低于卖单第一档价格，不再匹配
                break;
            } else if (takerOrder.direction == DirectionEnum.SELL && makerOrder.price.compareTo(takerOrder.price) < 0) {
                // 卖单价格高于买单第一档价格，不再匹配
                break;
            }
            // 成交价格
            this.marketPrice = makerOrder.price;
            // 成交数量
            BigDecimal matchQuantity = takerUnfilledQuantity.min(makerOrder.unfilledQuantity);
            // 更新成交记录
            matchResult.add(makerOrder.price, matchQuantity, makerOrder);
            // 更新订单状态
            takerUnfilledQuantity = takerUnfilledQuantity.subtract(matchQuantity);
            BigDecimal makeUnfilledQuantity = makerOrder.unfilledQuantity.subtract(matchQuantity);

            // 对手盘完全成交后，从订单簿中删除
            if (makeUnfilledQuantity.signum() == 0) {
                makerOrder.updateOrder(makeUnfilledQuantity, OrderStatus.FULLY_FILLED, timeStamp);
                makerBook.remove(makerOrder);
            } else {
                // 对手盘部分成交后，更新订单状态
                makerOrder.updateOrder(makeUnfilledQuantity, OrderStatus.PARTIAL_FILLED, timeStamp);
            }

            // Taker完全成交后，退出循环
            if (takerUnfilledQuantity.signum() == 0) {
                takerOrder.updateOrder(takerUnfilledQuantity, OrderStatus.FULLY_FILLED, timeStamp);
                break;
            }
        }
        // Taker未完全成交，放入对应的订单簿
        if (takerUnfilledQuantity.signum() > 0) {
            takerOrder.updateOrder(takerUnfilledQuantity,
                    takerUnfilledQuantity.compareTo(takerOrder.quantity) == 0 ? OrderStatus.PENDING : OrderStatus.PARTIAL_FILLED,
                    timeStamp);
            anotherBook.add(takerOrder);
        }
        return matchResult;
    }

    /**
     * 撤单，查询订单状态
     *
     * @param timeStamp
     * @param order
     */
    public void cancel(long timeStamp, Order order) {
        OrderBook book = order.direction == DirectionEnum.BUY ? this.buyBook : this.sellBook;
        if (!book.remove(order)) {
            throw new IllegalArgumentException("Order not found in order book: " + order);
        }
        OrderStatus status = order.unfilledQuantity.compareTo(order.quantity) == 0 ? OrderStatus.FULLY_CANCELLED : OrderStatus.PARTIAL_CANCELLED;
        order.updateOrder(order.unfilledQuantity, status, timeStamp);
    }
}
