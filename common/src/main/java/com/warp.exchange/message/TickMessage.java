package com.warp.exchange.message;

import com.warp.exchange.entity.quatation.TickEntity;

import java.util.List;

public class TickMessage extends AbstractMessage {
    
    public long sequenceId;
    
    public List<TickEntity> ticks;
}
