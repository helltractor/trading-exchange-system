package com.warp.exchange.support;

import com.warp.exchange.database.DataBaseTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 具有数据库支持的服务
 */
public abstract class AbstractDbService extends LoggerSupport {
    
    @Autowired
    protected DataBaseTemplate dataBase;
}
