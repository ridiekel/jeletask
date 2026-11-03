package io.github.ridiekel.jeletask.client.spec.config;

/**
 * Collects what is wrong with one component of the config file. The component itself is added to the message by the collector.
 */
public interface ConfigProblems {
    /**
     * A problem that keeps the bridge from starting.
     */
    void error(String property, String message);

    /**
     * A problem that is logged, after which the bridge starts anyway.
     */
    void warning(String property, String message);
}
