package com.oms.sim;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic, monotonic id generator. Seeded so the same seed produces the same id
 * sequence — useful for reproducible test runs.
 */
public final class IdGenerator {

    private final String prefixCl;
    private final String prefixOrder;
    private final String prefixExec;
    private final AtomicLong clCounter = new AtomicLong();
    private final AtomicLong orderCounter = new AtomicLong();
    private final AtomicLong execCounter = new AtomicLong();

    public IdGenerator(long seed) {
        // Seed influences only the prefix so ids stay sortable but distinct across runs.
        this.prefixCl = String.format("CL-%08x", seed & 0xffffffffL);
        this.prefixOrder = String.format("OID-%08x", seed & 0xffffffffL);
        this.prefixExec = String.format("EX-%08x", seed & 0xffffffffL);
    }

    public String nextClOrdId() {
        return prefixCl + "-" + clCounter.incrementAndGet();
    }

    public String nextOrderId() {
        return prefixOrder + "-" + orderCounter.incrementAndGet();
    }

    public String nextExecId() {
        return prefixExec + "-" + execCounter.incrementAndGet();
    }
}
