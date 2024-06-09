package com.warp.exchange.order;

import com.warp.exchange.enums.DirectionEnum;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * 订单簿
 */
public class OrderBook {

    public final DirectionEnum direction;
    public final TreeMap<OrderKey, Order> book;

    public OrderBook(DirectionEnum direction, TreeMap<OrderKey, Order> book) {
        this.direction = direction;
        this.book = book;
    }

    public OrderBook(DirectionEnum direction) {
        this.direction = direction;
        this.book = new TreeMap<>(direction == DirectionEnum.BUY ? SORT_BUY : SORT_SELL);

    }

    /**
     * 卖单排序
     * 价格低在前，价格相同时间早在前
     */
    public static final Comparator<OrderKey> SORT_SELL = new Comparator<>() {
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
       public int compare(OrderKey o1, OrderKey o2) {
           int cmp = o2.price().compareTo(o1.price());
           return cmp == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : cmp;
        }
    };

    /**
     * 获取首个订单
     * @return
     */
    public Order getFirst(){
        return this.book.isEmpty() ? null : this.book.firstEntry().getValue();
    }

    /**
     * 删除订单
     * @param order
     * @return
     */
    public boolean remove(Order order){
        return this.book.remove(new OrderKey(order.sequenceId, order.price)) != null;
    }

    /**
     * 添加订单
     * @param order
     * @return
     */
    public boolean add(Order order){
        return this.book.put(new OrderKey(order.sequenceId, order.price), order) == null;
    }
}
