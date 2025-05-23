package com.helltractor.exchange.message;

import java.util.List;

import com.helltractor.exchange.model.quatation.TickEntity;

public class TickMessage extends AbstractMessage {

    public long sequenceId;

    public List<TickEntity> ticks;
}
