package com.warp.exchange.support;

import com.warp.exchange.database.DataBaseTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service with data base support.
 */
public abstract class AbstractDbSupport extends LoggerSupport {
    
    @Autowired
    protected DataBaseTemplate dataBase;
}
