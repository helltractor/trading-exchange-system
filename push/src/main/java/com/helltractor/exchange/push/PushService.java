package com.helltractor.exchange.push;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.helltractor.exchange.redis.RedisCache;
import com.helltractor.exchange.support.LoggerSupport;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import io.vertx.redis.client.impl.types.BulkType;
import jakarta.annotation.PostConstruct;

/**
 * 用于初始化和管理与Redis服务器的连接和消息处理，通过Vertx框架实现异步操作，并将接收到的Redis推送消息广播给其他组件。
 */
@Component
public class PushService extends LoggerSupport {

    private Vertx vertx;

    @Value("${exchange.config.hmac-key}")
    private String hmacKey;

    @Value("${server.port}")
    private int serverPort;

    @Value("${spring.redis.standalone.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.standalone.port:6379}")
    private int redisPort;

    @Value("${spring.redis.standalone.password:}")
    private String redisPassword;

    @Value("${spring.redis.standalone.database:0}")
    private int redisDatabase;

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
                logger.error("subscribe failed.", err);
                exit(1);
            });
        }).onFailure(err -> {
            logger.error("connect to redis failed.", err);
            exit(1);
        });
    }

    void exit(int exitCode) {
        logger.warn("exit with code: {}", exitCode);

        if (this.vertx != null) {
            this.vertx.close(result -> {
                if (result.succeeded()) {
                    logger.info("vertx closed.");
                } else {
                    logger.error("vertx close failed.", result.cause());
                }
            });
            System.exit(exitCode);
        } else {
            System.exit(exitCode);
        }
    }
}
