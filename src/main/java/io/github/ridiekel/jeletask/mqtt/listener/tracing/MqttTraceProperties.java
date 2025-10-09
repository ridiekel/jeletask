package io.github.ridiekel.jeletask.mqtt.listener.tracing;

import java.time.Duration;

public class MqttTraceProperties {

    private Duration retention = Duration.ofDays(7);
    private Cleanup cleanup = new Cleanup();

    public Duration getRetention() {
        return retention;
    }

    public void setRetention(Duration retention) {
        this.retention = retention;
    }

    public Cleanup getCleanup() {
        return cleanup;
    }

    public void setCleanup(Cleanup cleanup) {
        this.cleanup = cleanup;
    }

    public static class Cleanup {
        private boolean enabled = true;
        private Duration interval = Duration.ofMinutes(15);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }
    }
}
