package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

import java.util.Map;
import java.util.function.Supplier;

public class MqttLogger {
    public static final Map<What, String> WHAT_LOG_PATTERNS = Map.of(
            What.PING, whatPattern(AnsiColor.BRIGHT_BLUE),
            What.PUBLISH, whatPattern(AnsiColor.YELLOW),
            What.RECEIVE, whatPattern(AnsiColor.MAGENTA),
            What.ONLINE, whatPattern(AnsiColor.BRIGHT_WHITE),
            What.CONFIG, whatPattern(AnsiColor.CYAN),
            What.DELETE, whatPattern(AnsiColor.BLUE)
    );

    private static String whatPattern(AnsiColor color) {
        return AnsiOutput.toString(color, "[MQTT      ] - [%s] - %s", AnsiColor.DEFAULT, " - %s");
    }

    public enum What {
        PING,
        PUBLISH,
        RECEIVE,
        ONLINE,
        CONFIG,
        DELETE
    }

    public static Supplier<String> getLoggingStringForComponent(ComponentSpec componentSpec) {
        return () -> String.format("[%s] - [%s] - [%s]", StringUtils.rightPad(componentSpec.getFunction().toString(), 10), StringUtils.leftPad(String.valueOf(componentSpec.getNumber()), 3), StringUtils.leftPad(componentSpec.getDescription(), 40));
    }

    public static String getWhat(What what) {
        return StringUtils.rightPad(what.toString(), 10);
    }

    public static String payloadToLogWithColors(String payload) {
        return AnsiOutput.toString(AnsiColor.GREEN, payload, AnsiColor.DEFAULT);
    }
}
