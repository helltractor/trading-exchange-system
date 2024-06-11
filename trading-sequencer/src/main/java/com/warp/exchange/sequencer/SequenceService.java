package com.warp.exchange.sequencer;

import com.warp.exchange.message.event.AbstractEvent;
import com.warp.exchange.messaging.*;
import com.warp.exchange.support.LoggerSupport;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sequence events
 */
@Component
public class SequenceService extends LoggerSupport implements CommonErrorHandler {
    
    private static final String GROUP_ID = "SequenceGroup";
    
    @Autowired
    private SequenceHandler sequenceHandler;
    
    @Autowired
    private MessagingFactory messagingFactory;
    
    @Autowired
    private MessageTypes messageTypes;
    
    private MessageProducer<AbstractEvent> messageProducer;
    
    private AtomicLong sequence;
    
    private Thread jobThread;
    
    private boolean crash = false;
    private boolean running;
    
    @PostConstruct
    void init() {
        Thread thread = new Thread(() -> {
            logger.info("start sequence service...");
            // TODO: try get global DB lock:
            // while (!hasLock()) { sleep(10000); }
            this.messageProducer = this.messagingFactory.createMessageProducer(Messaging.Topic.TRADE,
                    AbstractEvent.class);
            // find max event id
            this.sequence = new AtomicLong(this.sequenceHandler.getMaxSequenceId());
            // init consumer
            logger.info("create message consumer for {} ...", getClass().getName());
            // share same group id
            MessageConsumer consumer = this.messagingFactory.createBatchMessageListener(Messaging.Topic.SEQUENCE, GROUP_ID,
                    this::processMessage, this);
            // start running
            this.running = true;
            while (running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            // close message consumer
            logger.info("close message consumer for {} ...", getClass().getName());
            consumer.stop();
            System.exit(1);
        });
        this.jobThread = thread;
        this.jobThread.start();
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("shutdown sequence service...");
        running = false;
        if (jobThread != null) {
            jobThread.interrupt();
            try {
                jobThread.join(5000);
            } catch (InterruptedException e) {
                logger.error("interrupt job thread failed", e);
            }
            jobThread = null;
        }
    }
    
    /**
     * Message consumer error handler
     */
    @Override
    public void handleBatch(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer,
                            MessageListenerContainer container, Runnable invokeListener) {
        logger.error("batch error", thrownException);
        panic();
    }
    
    private synchronized void processMessage(List<AbstractEvent> messages) {
        if (!running || crash) {
            panic();
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("do sequence for {} messages...", messages.size());
        }
        List<AbstractEvent> sequenced = null;
        try {
            sequenced = this.sequenceHandler.sequenceMessages(this.messageTypes, this.sequence, messages);
        } catch (Throwable e) {
            logger.error("exception when do sequence", e);
            shutdown();
            panic();
            throw new Error(e);
        }
    }
    
    void panic() {
        this.crash = true;
        this.running = true;
        System.exit(1);
    }
}
