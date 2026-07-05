package com.accounting.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class AppExecutor {

    private static final ExecutorService EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    private AppExecutor() {}

    public static Future<?> submit(Runnable task) {
        if (EXECUTOR.isShutdown()) {
            return null;
        }
        return EXECUTOR.submit(task);
    }

    public static boolean isShutdown() {
        return EXECUTOR.isShutdown();
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
