package com.helltractor.exchange.store;

import com.helltractor.exchange.db.DbTemplate;
import com.helltractor.exchange.model.trade.EventEntity;
import com.helltractor.exchange.message.event.AbstractEvent;
import com.helltractor.exchange.messaging.MessageTypes;
import com.helltractor.exchange.model.support.EntitySupport;
import com.helltractor.exchange.support.LoggerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional
public class StoreService extends LoggerSupport {
    
    @Autowired
    MessageTypes messageTypes;
    
    @Autowired
    DbTemplate dbTemplate;
    
    public List<AbstractEvent> loadEventFromDb(long lastEventId) {
        List<EventEntity> events = this.dbTemplate.from(EventEntity.class)
                .where("sequenceId > ?", lastEventId)
                .orderBy("sequenceId")
                .limit(100000).list();
        return events.stream().map(event -> (AbstractEvent) messageTypes.deserialize(event.data))
                .collect(Collectors.toList());
    }
    
    public void insertIgnore(List<? extends EntitySupport> list) {
        dbTemplate.insertIgnore(list);
    }
}
