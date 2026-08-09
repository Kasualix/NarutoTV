package me.kall.narutotv.produce.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class TimeCostDebugger {
    private final int size;
    private final long[] costs;
    private volatile int timeIndex = 0;
    private volatile int count = 0;
    private volatile long lastPrint = -1;
    private final String reason;

    public TimeCostDebugger(int size, String reason) {
        this.size = size;
        this.costs = new long[size];
        this.reason = reason;
    }

    public synchronized void reset() {
        Arrays.fill(this.costs, 0L);
        this.timeIndex = 0;
        this.count = 0;
    }

    public synchronized void record(long nanoseconds) {
        this.costs[this.timeIndex] = nanoseconds;
        this.timeIndex = (this.timeIndex + 1) % this.size;
        if (this.count < this.size) {
            this.count++;
        }
    }

    public synchronized void printDebug() {
        if (System.nanoTime() - this.lastPrint < 2_000_000_000L) return;
        this.lastPrint = System.nanoTime();

        double[] costs = this.costsMillis();

        double total = 0D;
        for (double cost : costs) total += cost;
        double average = total / costs.length;

        System.out.println("[" + this.reason + "] Costs of the last " + this.count + " records in ms: " + Arrays.toString(costs) + ". Average: " + average + "ms.");
    }

    @Contract(pure = true)
    private double @NotNull [] costsMillis() {
        double[] millis = new double[this.count];
        if (this.count == 0) return millis;

        int start = (this.timeIndex - this.count + this.size) % this.size;
        for (int i = 0; i < this.count; i++) {
            millis[i] = this.costs[(start + i) % this.size] / 1_000_000.0;
        }
        return millis;
    }
}