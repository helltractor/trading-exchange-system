package com.warp.exchange.service;

import com.warp.exchange.bean.SimpleMatchDetailRecord;
import com.warp.exchange.entity.trade.MatchDetailEntity;
import com.warp.exchange.entity.trade.OrderEntity;
import com.warp.exchange.support.AbstractDbService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通过数据库查询用户的历史订单
 */
@Component
public class HistoryService extends AbstractDbService {
    
    /**
     * 获取历史订单列表
     *
     * @param userId
     * @param maxResults
     * @return
     */
    public List<OrderEntity> getHistoryOrders(Long userId, int maxResults) {
        return dataBase.from(OrderEntity.class)
                .where("userId = ?", userId)
                .orderBy("id")
                .desc()
                .limit(maxResults)
                .list();
    }
    
    /**
     * 获取历史订单详情
     *
     * @param userId
     * @param orderId
     * @return
     */
    public OrderEntity getHistoryOrder(Long userId, Long orderId) {
        OrderEntity entity = dataBase.fetch(OrderEntity.class, orderId);
        if (entity == null || !entity.userId.equals(userId)) {
            return null;
        }
        return entity;
    }
    
    /**
     * 获取历史订单的撮合明细
     *
     * @param orderId
     * @return
     */
    public List<SimpleMatchDetailRecord> getHistoryMatchDetails(Long orderId) {
        List<MatchDetailEntity> details = dataBase.select("price", "quantity", "type")
                .from(MatchDetailEntity.class)
                .where("orderId = ?", orderId)
                .orderBy("id")
                .list();
        return details.stream().map(e -> new SimpleMatchDetailRecord(e.price, e.quantity, e.type)).collect(Collectors.toList());
    }
}
