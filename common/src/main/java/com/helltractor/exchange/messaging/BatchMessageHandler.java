package com.helltractor.exchange.messaging;

import java.util.List;

import com.helltractor.exchange.message.AbstractMessage;

@FunctionalInterface
public interface BatchMessageHandler<T extends AbstractMessage> {

    void processMessages(List<T> messages);
}
