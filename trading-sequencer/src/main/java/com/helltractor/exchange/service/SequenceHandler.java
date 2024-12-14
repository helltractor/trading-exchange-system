package com.helltractor.exchange.service;

import com.helltractor.exchange.message.event.AbstractEvent;
import com.helltractor.exchange.messaging.MessageTypes;
import com.helltractor.exchange.model.trade.EventEntity;
import com.helltractor.exchange.model.trade.UniqueEventEntity;
import com.helltractor.exchange.support.AbstractDbService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Process events as batch.
 */
@Component
@Transactional(rollbackFor = Throwable.class)
public class SequenceHandler extends AbstractDbService {
    
    long lastTimestamp = 0;
    
    public long getMaxSequenceId() {
        EventEntity last = dataBase.from(EventEntity.class).orderBy("sequenceId").desc().first();
        if (last == null) {
            logger.info("no max sequenceId found. set max sequenceId to 0.");
            return 0;
        }
        this.lastTimestamp = last.createTime;
        logger.info("find max sequenceId = {}, last timestamp = {}", last.sequenceId, this.lastTimestamp);
        return last.sequenceId;
    }
    
    /**
     * Set sequence for each message, persist into database as batch.
     */
    public List<AbstractEvent> sequenceMessages(final MessageTypes messageTypes, final AtomicLong sequence,
                                                final List<AbstractEvent> messages) {
        final long timestamp = System.currentTimeMillis();
        if (timestamp < this.lastTimestamp) {
            logger.warn("[Sequence] current time {} is turned back from {}!", timestamp, this.lastTimestamp);
        } else {
            this.lastTimestamp = timestamp;
        }
        List<UniqueEventEntity> uniques = null;
        Set<String> uniqueKeys = null;
        List<AbstractEvent> sequenceMessages = new ArrayList<>(messages.size());
        List<EventEntity> events = new ArrayList<>(messages.size());
        for (AbstractEvent message : messages) {
            UniqueEventEntity unique = null;
            final String uniqueId = message.uniqueId;
            // check uniqueId
            if (uniqueId != null) {
                if ((uniqueKeys != null && uniqueKeys.contains(uniqueId)) ||
                        dataBase.fetch(UniqueEventEntity.class, uniqueId) != null) {
                    logger.warn("ignore processed unique message: {}", message);
                    continue;
                }
                unique = new UniqueEventEntity();
                unique.uniqueId = uniqueId;
                unique.createTime = message.createTime;
                if (uniques == null) {
                    uniques = new ArrayList<>();
                }
                uniques.add(unique);
                if (uniqueKeys == null) {
                    uniqueKeys = new HashSet<>();
                }
                uniqueKeys.add(uniqueId);
                logger.info("unique event {} sequenced.", uniqueId);
            }
            
            final long previousId = sequence.get();
            final long currentId = sequence.incrementAndGet();
            // 先设置message的sequenceId / previousId / createTime，再序列化并落库
            message.sequenceId = currentId;
            message.previousId = previousId;
            message.createTime = this.lastTimestamp;
            // 如果此消息关联了UniqueEvent，给UniqueEvent加上相同的sequenceId
            if (unique != null) {
                unique.sequenceId = message.sequenceId;
            }
            // create AbstractEvent and save to db later
            EventEntity event = new EventEntity();
            event.previousId = previousId;
            event.sequenceId = currentId;
            event.data = messageTypes.serialize(message);
            event.createTime = this.lastTimestamp; // same as message.createTime
            events.add(event);
            // will send later
            sequenceMessages.add(message);
        }
        
        if (uniques != null) {
            dataBase.insert(uniques);
        }
        dataBase.insert(events);
        return sequenceMessages;
    }
}
