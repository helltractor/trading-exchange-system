package com.helltractor.exchange.messaging;

/**
 * Messaging topics.
 */
public interface Messaging {

    enum Topic {

        /**
         * Topic name: to sequence.
         */
        SEQUENCE(1),

        /**
         * Topic name: to/from trading-engine.
         */
        TRANSFER(1),

        /**
         * Topic name: events to trading-engine.
         */
        TRADE(1),

        /**
         * Topic name: tick to quotation for generate bars.
         */
        TICK(1);

        final int concurrency;

        Topic(int concurrency) {
            this.concurrency = concurrency;
        }

        public int getConcurrency() {
            return this.concurrency;
        }

        public int getPartitions() {
            return this.concurrency;
        }
    }
}
