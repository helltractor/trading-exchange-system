package com.helltractor.exchange.support;

import org.springframework.beans.factory.annotation.Autowired;

import com.helltractor.exchange.db.DbTemplate;

public abstract class AbstractDbService extends LoggerSupport {

    @Autowired
    protected DbTemplate dataBase;
}
