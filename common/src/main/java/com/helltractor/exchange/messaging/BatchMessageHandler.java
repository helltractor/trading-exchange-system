package com.helltractor.exchange.messaging;

import com.helltractor.exchange.message.AbstractMessage;

import java.util.List;

@FunctionalInterface
public interface BatchMessageHandler<T extends AbstractMessage> {
    
    void processMessages(List<T> messages);
}
