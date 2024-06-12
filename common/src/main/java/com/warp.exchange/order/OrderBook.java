package com.warp.exchange.order;

import com.warp.exchange.bean.OrderBookItemBean;
import com.warp.exchange.entity.trade.OrderEntity;
import com.warp.exchange.enums.Direction;

import java.util.*;

/**
 * 订单簿
 */
public class OrderBook {
    
    /**
     * 卖单排序
     * 价格低在前，价格相同时间早在前
     */
    public static final Comparator<OrderKey> SORT_SELL = new Comparator<>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            int cmp = o1.price().compareTo(o2.price());
            return cmp == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : cmp;
        }
    };
    /**
     * 买单排序
     * 价格高在前，价格相同时间早在前
     */
    public static final Comparator<OrderKey> SORT_BUY = new Comparator<>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            int cmp = o2.price().compareTo(o1.price());
            return cmp == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : cmp;
        }
    };
    public final Direction direction;
    public final TreeMap<OrderKey, OrderEntity> book;
    
    public OrderBook(Direction direction, TreeMap<OrderKey, OrderEntity> book) {
        this.direction = direction;
        this.book = book;
    }
    
    public OrderBook(Direction direction) {
        this.direction = direction;
        this.book = new TreeMap<>(direction == Direction.BUY ? SORT_BUY : SORT_SELL);
        
    }
    
    /**
     * 获取首个订单
     *
     * @return
     */
    public OrderEntity getFirst() {
        return this.book.isEmpty() ? null : this.book.firstEntry().getValue();
    }
    
    /**
     * 删除订单
     *
     * @param order
     * @return
     */
    public boolean remove(OrderEntity order) {
        return this.book.remove(new OrderKey(order.sequenceId, order.price)) != null;
    }
    
    /**
     * 添加订单
     *
     * @param order
     * @return
     */
    public boolean add(OrderEntity order) {
        return this.book.put(new OrderKey(order.sequenceId, order.price), order) == null;
    }
    
    public boolean exist(OrderEntity order) {
        return this.book.containsKey(new OrderKey(order.sequenceId, order.price));
    }
    
    public int size() {
        return this.book.size();
    }
    
    public List<OrderBookItemBean> getOrderBook(int maxDepth) {
        List<OrderBookItemBean> items = new ArrayList<>();
        OrderBookItemBean preItem = null;
        for (OrderKey key : this.book.keySet()) {
            OrderEntity order = this.book.get(key);
            if (preItem == null) {
                preItem = new OrderBookItemBean(order.price, order.unfilledQuantity);
                items.add(preItem);
            } else {
                if (order.price.compareTo(preItem.price) == 0) {
                    preItem.addQuantity(order.unfilledQuantity);
                } else {
                    if (items.size() >= maxDepth) {
                        break;
                    }
                    preItem = new OrderBookItemBean(order.price, order.unfilledQuantity);
                    items.add(preItem);
                }
            }
        }
        return items;
    }
    
    @Override
    public String toString() {
        if (this.book.isEmpty()) {
            return "(empty)";
        }
        List<String> orders = new ArrayList<>(10);
        for (Map.Entry<OrderKey, OrderEntity> entry : this.book.entrySet()) {
            OrderEntity order = entry.getValue();
            orders.add("  " + order.price + " " + order.unfilledQuantity + " " + order.toString());
        }
        if (direction == Direction.SELL) {
            Collections.reverse(orders);
        }
        return String.join("\n", orders);
    }
}
