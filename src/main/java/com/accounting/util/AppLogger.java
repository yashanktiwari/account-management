package com.accounting.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppLogger {

    private AppLogger() {}

    public static Logger get(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
