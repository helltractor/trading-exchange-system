package com.helltractor.exchange.message;

import com.helltractor.exchange.entity.quatation.TickEntity;

import java.util.List;

public class TickMessage extends AbstractMessage {
    
    public long sequenceId;
    
    public List<TickEntity> ticks;
}
