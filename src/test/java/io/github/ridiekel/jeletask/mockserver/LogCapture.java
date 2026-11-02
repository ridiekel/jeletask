package io.github.ridiekel.jeletask.mockserver;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sets the level of a logger for the duration of a test and keeps what it logs, instead of printing it.
 */
public final class LogCapture implements AutoCloseable {
    private final Logger logger;
    private final Level previousLevel;
    private final boolean previousAdditive;
    private final List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
    private final AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
        @Override
        protected void append(ILoggingEvent event) {
            LogCapture.this.events.add(event);
        }
    };

    private LogCapture(String name, Level level) {
        this.logger = (Logger) LoggerFactory.getLogger(name);
        this.previousLevel = this.logger.getLevel();
        this.previousAdditive = this.logger.isAdditive();
        this.appender.start();
        this.logger.addAppender(this.appender);
        this.logger.setAdditive(false);
        this.logger.setLevel(level);
    }

    public static LogCapture of(Class<?> type, Level level) {
        return new LogCapture(type.getName(), level);
    }

    /**
     * Everything the client logs goes here instead of to the console, because a client without Spring logs at DEBUG.
     */
    public static LogCapture quietClient() {
        return new LogCapture("io.github.ridiekel.jeletask.client", Level.INFO);
    }

    public List<String> messages(Level level) {
        return this.events.stream().filter(event -> event.getLevel() == level).map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Override
    public void close() {
        this.logger.detachAppender(this.appender);
        this.appender.stop();
        this.logger.setAdditive(this.previousAdditive);
        this.logger.setLevel(this.previousLevel);
    }
}
