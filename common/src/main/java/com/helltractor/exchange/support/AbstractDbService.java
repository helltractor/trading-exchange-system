package com.helltractor.exchange.support;

import com.helltractor.exchange.db.DbTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDbService extends LoggerSupport {
    
    @Autowired
    protected DbTemplate dataBase;
}
