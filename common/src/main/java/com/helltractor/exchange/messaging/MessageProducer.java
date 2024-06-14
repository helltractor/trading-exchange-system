package com.helltractor.exchange.messaging;

import com.helltractor.exchange.message.AbstractMessage;

import java.util.List;

@FunctionalInterface
public interface MessageProducer<T extends AbstractMessage> {
    
    default void sendMessages(List<T> messages) {
        for (T message : messages) {
            sendMessages(message);
        }
    }
    
    void sendMessages(T message);
    
}
