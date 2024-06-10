package com.warp.exchange.messaging;

import com.warp.exchange.message.AbstractMessage;

import java.util.List;

@FunctionalInterface
public interface BatchMessageHandler<T extends AbstractMessage> {
    
    void processMessages(List<T> messages);
}