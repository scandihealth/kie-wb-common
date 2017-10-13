package com.dxc.drools.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keep track of execution time and logs it.
 */
public class DroolsTimeLogger {
    private long startNano;

    private LogWrapper logWrapper = new LogWrapper();

    public void start() {
        this.startNano = System.nanoTime();
    }

    public void log(String loggerName, String methodName) {
        Logger logger = logWrapper.getLogger(loggerName);
        if (logger.isInfoEnabled()) {
            logger.info(String.format("| %s | %10.0f| ms|", new Object[]{methodName, (float) (System.nanoTime() - this.startNano) / 1000000.0F}));
        } else {
            logger.warn("Cannot log time as level info is not enabled");
        }
    }

    class LogWrapper {
        public Logger getLogger(String loggerName) {
            return LoggerFactory.getLogger(loggerName);
        }
    }

}
