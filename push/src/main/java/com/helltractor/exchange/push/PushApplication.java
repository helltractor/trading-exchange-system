package com.helltractor.exchange.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * WebSocket push based on VertX.
 */
// forbid database auto-configuration (no DataSource, JdbcTemplate...)
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class PushApplication {
    
    public static void main(String[] args) {
        System.setProperty("vertx.disableFileCPResolving", "true");
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        SpringApplication app = new SpringApplication(PushApplication.class);
        // forbid Spring web
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
