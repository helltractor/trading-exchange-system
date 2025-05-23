package com.helltractor.exchange.messaging;

import java.util.List;

import com.helltractor.exchange.message.AbstractMessage;

@FunctionalInterface
public interface MessageProducer<T extends AbstractMessage> {

    default void sendMessages(List<T> messages) {
        for (T message : messages) {
            sendMessages(message);
        }
    }

    void sendMessages(T message);
}
