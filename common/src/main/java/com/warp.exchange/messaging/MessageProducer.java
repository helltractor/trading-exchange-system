package com.warp.exchange.messaging;

import com.warp.exchange.message.AbstractMessage;

import java.util.List;

@FunctionalInterface
public interface MessageProducer<T extends AbstractMessage> {
    
    void sendMessages(T message);
    
    default void sendMessages(List<T> messages) {
        for (T message : messages) {
            sendMessages(message);
        }
    }
}
