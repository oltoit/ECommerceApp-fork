package com.github.damiox.ecommerce.api.controller.performance.timeBehaviour;

public class PerfTimer {

    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();
    private static final ThreadLocal<Long> duration = new ThreadLocal<>();

    public static void start() {
        startTime.set(System.currentTimeMillis());
    }

    public static void stop() {
        duration.set(System.currentTimeMillis() - startTime.get());
    }

    public static long getDuration() {
        return duration.get();
    }

    public static void reset() {
        startTime.set(Long.MIN_VALUE);
        duration.set(Long.MIN_VALUE);
    }
}