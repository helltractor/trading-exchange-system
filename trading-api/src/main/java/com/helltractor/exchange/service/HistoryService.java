package com.helltractor.exchange.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.helltractor.exchange.bean.SimpleMatchDetailRecord;
import com.helltractor.exchange.model.trade.MatchDetailEntity;
import com.helltractor.exchange.model.trade.OrderEntity;
import com.helltractor.exchange.support.AbstractDbService;

/**
 * Find history details service.
 */
@Component
public class HistoryService extends AbstractDbService {

    /**
     * Get history orders by userId.
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
     * Get history order by userId and orderId.
     */
    public OrderEntity getHistoryOrder(Long userId, Long orderId) {
        OrderEntity entity = dataBase.fetch(OrderEntity.class, orderId);
        if (entity == null || !entity.userId.equals(userId)) {
            return null;
        }
        return entity;
    }

    /**
     * Get history match details by orderId.
     */
    public List<SimpleMatchDetailRecord> getHistoryMatchDetails(Long orderId) {
        List<MatchDetailEntity> details = dataBase.select("price", "quantity", "type")
                .from(MatchDetailEntity.class)
                .where("orderId = ?", orderId)
                .orderBy("id")
                .list();
        return details.stream().map(e
                -> new SimpleMatchDetailRecord(e.price, e.quantity, e.type))
                .collect(Collectors.toList());
    }
}
