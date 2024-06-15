package com.warp.exchange.push;

import com.warp.exchange.redis.RedisCache;
import com.warp.exchange.support.LoggerSupport;
import io.vertx.core.Vertx;
import io.vertx.redis.client.*;
import io.vertx.redis.client.impl.types.BulkType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PushService extends LoggerSupport {
    
    @Value("${exchange.config.hmac-key}")
    String hmacKey;
    
    @Value("${server.port}")
    int serverPort;
    
    @Value("${spring.redis.standalone.host:localhost}")
    String redisHost;
    
    @Value("${spring.redis.standalone.port:6379}")
    int redisPort;
    
    @Value("${spring.redis.standalone.password:}")
    String redisPassword;
    
    @Value("${spring.redis.standalone.database:0}")
    int redisDatabase = 0;
    
    Vertx vertx;
    
    @PostConstruct
    public void startVertx() {
        logger.info("start vertx...");
        this.vertx = Vertx.vertx();
        
        var push = new PushVerticle(this.hmacKey, this.serverPort);
        vertx.deployVerticle(push);
        
        String url = "redis://" + (this.redisPassword.isEmpty() ? "" : ":" + this.redisPassword + "@") + this.redisHost
                + ":" + this.redisPort + "/" + this.redisDatabase;
        
        logger.info("create redis client: {}", url);
        Redis redis = Redis.createClient(vertx, url);
        
        redis.connect().onSuccess(conn -> {
            logger.info("connected to redis server.");
            conn.handler(response -> {
                if (response.type() == ResponseType.PUSH) {
                    int size = response.size();
                    if (size == 3) {
                        Response type = response.get(2);
                        if (type instanceof BulkType) {
                            String message = type.toString();
                            if (logger.isDebugEnabled()) {
                                logger.debug("received push message: {}", message);
                            }
                            push.broadcast(message);
                        }
                    }
                }
            });
            logger.info("try subscribe...");
            conn.send(Request.cmd(Command.SUBSCRIBE).arg(RedisCache.Topic.NOTIFICATION)).onSuccess(res -> {
                logger.info("subscribe success.");
            }).onFailure(err -> {
                logger.error("subscribe failed.");
                System.exit(1);
            });
        }).onFailure(err -> {
            logger.error("connect to redis failed.");
            System.exit(1);
        });
    }
}
