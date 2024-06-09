package com.warp.exchange.match;

import com.warp.exchange.order.Order;
import com.warp.exchange.enums.DirectionEnum;
import com.warp.exchange.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单撮合引擎测试
 */
public class MatchEngineTest {

    static Long USER_A = 12345L;
    long sequenceId = 0;
    MatchEngine engine;

    @BeforeEach
    void init() {
        this.engine = new MatchEngine();
    }

    @Test
    void processOrders() {
        List<Order> orders = List.of( //
                createOrder(DirectionEnum.BUY, "12300.21", "1.02"), // 0
                createOrder(DirectionEnum.BUY, "12305.39", "0.33"), // 1
                createOrder(DirectionEnum.SELL, "12305.39", "0.11"), // 2
                createOrder(DirectionEnum.SELL, "12300.01", "0.33"), // 3
                createOrder(DirectionEnum.SELL, "12400.00", "0.10"), // 4
                createOrder(DirectionEnum.SELL, "12400.00", "0.20"), // 5
                createOrder(DirectionEnum.SELL, "12390.00", "0.15"), // 6
                createOrder(DirectionEnum.BUY, "12400.01", "0.55"), // 7
                createOrder(DirectionEnum.BUY, "12300.00", "0.77")); // 8
        List<MatchDetailRecord> matches = new ArrayList<>();
        for (Order order : orders) {
            MatchResult mr = this.engine.processOrder(order.sequenceId, order);
            matches.addAll(mr.matchDetails);
        }
        assertArrayEquals(new MatchDetailRecord[] { //
                new MatchDetailRecord(bd("12305.39"), bd("0.11"), orders.get(2), orders.get(1)), //
                new MatchDetailRecord(bd("12305.39"), bd("0.22"), orders.get(3), orders.get(1)), //
                new MatchDetailRecord(bd("12300.21"), bd("0.11"), orders.get(3), orders.get(0)), //
                new MatchDetailRecord(bd("12390.00"), bd("0.15"), orders.get(7), orders.get(6)), //
                new MatchDetailRecord(bd("12400.00"), bd("0.10"), orders.get(7), orders.get(4)), //
                new MatchDetailRecord(bd("12400.00"), bd("0.20"), orders.get(7), orders.get(5)), //
        }, matches.toArray(MatchDetailRecord[]::new));
        assertTrue(bd("12400.00").compareTo(engine.marketPrice) == 0);
    }

    Order createOrder(DirectionEnum DirectionEnum, String price, String quantity) {
        this.sequenceId++;
        var order = new Order();
        order.orderId = this.sequenceId << 4;
        order.sequenceId = this.sequenceId;
        order.direction = DirectionEnum;
        order.price = bd(price);
        order.quantity = order.unfilledQuantity = bd(quantity);
        order.status = OrderStatus.PENDING;
        order.userId = USER_A;
        order.createTime = order.updateTime = 1234567890000L + this.sequenceId;
        return order;
    }

    BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

}
