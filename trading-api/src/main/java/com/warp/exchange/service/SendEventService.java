package com.warp.exchange.service;

import com.warp.exchange.message.event.AbstractEvent;
import com.warp.exchange.messaging.MessageProducer;
import com.warp.exchange.messaging.Messaging;
import com.warp.exchange.messaging.MessagingFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 发送事件服务
 */
@Component
public class SendEventService {
    
    @Autowired
    MessagingFactory messagingFactory;
    
    MessageProducer<AbstractEvent> messageProducer;
    
    @PostConstruct
    public void init() {
        this.messageProducer = this.messagingFactory.createMessageProducer(Messaging.Topic.SEQUENCE, AbstractEvent.class);
    }
    
    public void sendMessage(AbstractEvent message) {
        this.messageProducer.sendMessages(message);
    }
}
