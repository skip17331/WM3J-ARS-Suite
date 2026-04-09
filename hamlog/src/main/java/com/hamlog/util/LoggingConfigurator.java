package com.hamlog.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configures Logback programmatically.
 *
 * Normal mode  → WARN and above only
 * Debug mode   → DEBUG (all CI-V traffic, DB queries, UI events, etc.)
 */
public class LoggingConfigurator {

    public static void configure(boolean debug) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.reset();

        Path logDir = Paths.get(System.getProperty("user.home"), ".hamlog", "logs");
        logDir.toFile().mkdirs();

        // Rolling file appender
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(ctx);
        fileAppender.setFile(logDir.resolve("hamlog.log").toString());

        TimeBasedRollingPolicy<ILoggingEvent> rolling = new TimeBasedRollingPolicy<>();
        rolling.setContext(ctx);
        rolling.setParent(fileAppender);
        rolling.setFileNamePattern(logDir.resolve("hamlog.%d{yyyy-MM-dd}.log").toString());
        rolling.setMaxHistory(7);
        rolling.start();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(ctx);
        encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();

        fileAppender.setRollingPolicy(rolling);
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        // Console appender
        ch.qos.logback.core.ConsoleAppender<ILoggingEvent> consoleAppender =
            new ch.qos.logback.core.ConsoleAppender<>();
        consoleAppender.setContext(ctx);
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(debug ? Level.DEBUG : Level.WARN);
        root.addAppender(fileAppender);
        root.addAppender(consoleAppender);

        // Always show INFO for the hamlog package regardless of mode
        Logger hamlogLogger = ctx.getLogger("com.hamlog");
        hamlogLogger.setLevel(debug ? Level.DEBUG : Level.INFO);
    }
}
