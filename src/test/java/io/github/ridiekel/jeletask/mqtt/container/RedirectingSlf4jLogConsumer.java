package io.github.ridiekel.jeletask.mqtt.container;

import org.slf4j.LoggerFactory;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public class RedirectingSlf4jLogConsumer extends Slf4jLogConsumer {
    public RedirectingSlf4jLogConsumer(Class<?> aClass, AnsiColor color, String name) {
        super(LoggerFactory.getLogger(aClass), true);
        withPrefix(AnsiOutput.toString(color, name, AnsiColor.DEFAULT));
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        if (outputFrame.getType() == OutputFrame.OutputType.END) {
            super.accept(outputFrame);
        } else {
            super.accept(new OutputFrame(OutputFrame.OutputType.STDOUT, outputFrame.getBytes()));
        }
    }
}
