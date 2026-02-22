package io.github.ridiekel.jeletask;

import com.codeborne.selenide.SelenideElement;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.ridiekel.jeletask.client.spec.*;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.*;
import io.github.ridiekel.jeletask.mqtt.container.ha.Config;
import io.github.ridiekel.jeletask.mqtt.container.ha.Entity;
import io.github.ridiekel.jeletask.mqtt.container.ha.HAObject;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import io.github.ridiekel.jeletask.mqtt.listener.logic.input.LongPressInputCaptor;
import io.github.ridiekel.jeletask.mqtt.listener.logic.motor.Progress;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class NativeRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerJeletaskHints(hints);
        registerTestcontainersHints(hints);
        registerSelenideHints(hints);
    }

    private static void registerJeletaskHints(RuntimeHints hints) {
        hints.resources().registerPattern("test-config.json");
        hints.resources().registerPattern("haconfig/*");

        hints.reflection().registerType(Progress.class, MemberCategory.values());
        hints.reflection().registerType(LongPressInputCaptor.class, MemberCategory.values());

        hints.reflection().registerType(DimmerState.class, MemberCategory.values());
        hints.reflection().registerType(DisplayMessageState.class, MemberCategory.values());
        hints.reflection().registerType(GasState.class, MemberCategory.values());
        hints.reflection().registerType(HumidityState.class, MemberCategory.values());
        hints.reflection().registerType(InputState.class, MemberCategory.values());
        hints.reflection().registerType(LogState.class, MemberCategory.values());
        hints.reflection().registerType(LuxState.class, MemberCategory.values());
        hints.reflection().registerType(MotorState.class, MemberCategory.values());
        hints.reflection().registerType(OnOffState.class, MemberCategory.values());
        hints.reflection().registerType(ProtectionState.class, MemberCategory.values());
        hints.reflection().registerType(PulseCounterState.class, MemberCategory.values());
        hints.reflection().registerType(StringState.class, MemberCategory.values());
        hints.reflection().registerType(TemperatureControlState.class, MemberCategory.values());
        hints.reflection().registerType(TemperatureState.class, MemberCategory.values());
        hints.reflection().registerType(State.class, MemberCategory.values());

        hints.reflection().registerType(CentralUnit.class, MemberCategory.values());
        hints.reflection().registerType(CentralUnitFactory.class, MemberCategory.values());
        hints.reflection().registerType(CentralUnitType.class, MemberCategory.values());
        hints.reflection().registerType(Command.class, MemberCategory.values());
        hints.reflection().registerType(ComponentSpec.class, MemberCategory.values());
        hints.reflection().registerType(Device.class, MemberCategory.values());
        hints.reflection().registerType(Function.class, MemberCategory.values());
        hints.reflection().registerType(RoomSpec.class, MemberCategory.values());

        hints.reflection().registerType(MqttProcessor.ConnectedStatus.class, MemberCategory.values());

        hints.reflection().registerType(Config.class, MemberCategory.values());
        hints.reflection().registerType(Entity.class, MemberCategory.values());
        hints.reflection().registerType(HAObject.class, MemberCategory.values());
        hints.reflection().registerType(Entity.Attributes.class, MemberCategory.values());
    }

    private static void registerSelenideHints(RuntimeHints hints) {
        hints.resources().registerPattern("com/codeborne/**");
        hints.resources().registerPattern("org/openqa/selenium/**");
        hints.resources().registerPattern("META-INF/defaultservices/.*");

        hints.proxies().registerJdkProxy(SelenideElement.class);
        try (
                var classes = new ClassGraph().acceptPackages("com.codeborne", "org.openqa.selenium").enableClassInfo().scan();
                var resources = new ClassGraph().scan()
        ) {
            for (Resource r : resources.getResourcesWithExtension("js")) {
                hints.resources().registerPattern(r.getPath());
            }
            for (Resource r : resources.getResourcesWithExtension("properties")) {
                hints.resources().registerPattern(r.getPath());
            }
            classes.getAllClasses().forEach(classInfo -> {
                try {
                    hints.reflection().registerType(classInfo.loadClass(), MemberCategory.values());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
        }
    }

    //Needed for now because otherwise the test containers library does not create a network alias for mqtt
    private static void registerTestcontainersHints(RuntimeHints hints) {
        hints.resources().registerPattern("org/testcontainers/**");

        try (var result = new ClassGraph().acceptPackages("org.testcontainers").enableClassInfo().scan()) {
            result.getAllClasses().forEach(classInfo -> {
                try {
                    hints.reflection().registerType(classInfo.loadClass(), MemberCategory.values());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
        }
    }
}
