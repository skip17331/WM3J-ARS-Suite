package com.hamradio.jbridge;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * LoggingConfigurator — programmatic Logback setup.
 *
 * Mirrors j-log's LoggingConfigurator exactly:
 *   - Normal mode: WARN on root, INFO on com.hamradio
 *   - Debug mode : DEBUG everywhere
 *   - Rolling file at ~/.hamlog/logs/j-bridge.log (shares the j-log log dir)
 *   - 7-day rotation
 */
public class LoggingConfigurator {

    public static void configure(boolean debug) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.reset();

        // Shared log directory with the rest of the ARS suite
        Path logDir = Paths.get(System.getProperty("user.home"), ".hamlog", "logs");
        logDir.toFile().mkdirs();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(ctx);
        encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();

        // Rolling file appender
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(ctx);
        fileAppender.setFile(logDir.resolve("j-bridge.log").toString());

        TimeBasedRollingPolicy<ILoggingEvent> rolling = new TimeBasedRollingPolicy<>();
        rolling.setContext(ctx);
        rolling.setParent(fileAppender);
        rolling.setFileNamePattern(logDir.resolve("j-bridge.%d{yyyy-MM-dd}.log").toString());
        rolling.setMaxHistory(7);
        rolling.start();

        fileAppender.setRollingPolicy(rolling);
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        // Console appender
        ConsoleAppender<ILoggingEvent> console = new ConsoleAppender<>();
        console.setContext(ctx);
        console.setEncoder(encoder);
        console.start();

        // Root logger
        Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(debug ? Level.DEBUG : Level.WARN);
        root.addAppender(fileAppender);
        root.addAppender(console);

        // Always INFO for our own package regardless of debug flag
        Logger appLogger = ctx.getLogger("com.hamradio.jbridge");
        appLogger.setLevel(debug ? Level.DEBUG : Level.INFO);

        // Quieten noisy third-party loggers
        ctx.getLogger("org.java_websocket").setLevel(Level.WARN);
    }
}
