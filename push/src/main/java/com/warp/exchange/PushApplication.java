package com.warp.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 基于VertX的推送服务（WebSocket）
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})   // 禁用数据库自动配置 (无DataSource, JdbcTemplate...)
public class PushApplication {
    public static void main(String[] args) {
        System.setProperty("vertx.disableFileCPResolving", "true");
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        SpringApplication app = new SpringApplication(PushApplication.class);
        // 禁用Spring中的Web功能
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
