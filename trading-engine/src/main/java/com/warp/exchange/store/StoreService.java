package com.warp.exchange.store;

import com.warp.exchange.database.DataBaseTemplate;
import com.warp.exchange.entity.EventEntity;
import com.warp.exchange.message.event.AbstractEvent;
import com.warp.exchange.messaging.MessageTypes;
import com.warp.exchange.support.EntitySupport;
import com.warp.exchange.support.LoggerSupport;
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
    DataBaseTemplate dataBaseTemplate;
    
    public List<AbstractEvent> loadEventFromDb(long lastEventId) {
        List<EventEntity> events = this.dataBaseTemplate.from(EventEntity.class)
                .where("sequenceId > ?", lastEventId)
                .orderBy("sequenceId")
                .limit(100000).list();
        return events.stream().map(event -> (AbstractEvent) messageTypes.deserialize(event.data))
                .collect(Collectors.toList());
    }
    
    public void insertIgnore(List<? extends EntitySupport> list) {
        dataBaseTemplate.insertIgnore(list);
    }
}
