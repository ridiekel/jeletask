package io.github.ridiekel.jeletask;

import com.codeborne.selenide.SelenideElement;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
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

public class NativeTestRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerJeletaskHints(hints);
        registerSelenideHints(hints);
        registerTestContainersHints(hints);
    }

    private static void registerSelenideHints(RuntimeHints hints) {
        hints.resources().registerPattern("com/codeborne/**");
        hints.resources().registerPattern("org/openqa/selenium/**");
        hints.resources().registerPattern("META-INF/defaultservices/.*");

        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.WebDriverContainer");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.ClipboardService");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.commands.Clear");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.commands.Commands");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.ex.ErrorFormatter");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.AttachmentHandler");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.DownloadFileToFolder");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.DownloadFileWithCdp");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.ElementCommunicator");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.ElementDescriber");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.PageObjectFactory");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.PageSourceExtractor");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.Photographer");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.ScreenShotLaboratory");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.impl.WebElementSelector");
        hints.resources().registerPattern("META-INF/defaultservices/com.codeborne.selenide.proxy.SelenideProxyServerFactory");

        try (ScanResult sr = new ClassGraph().scan()) {
            for (Resource r : sr.getResourcesWithExtension("js")) {
                hints.resources().registerPattern(r.getPath());
            }
            for (Resource r : sr.getResourcesWithExtension("properties")) {
                hints.resources().registerPattern(r.getPath());
            }
        }

        hints.proxies().registerJdkProxy(SelenideElement.class);
        try (var result = new ClassGraph().acceptPackages("org.openqa.selenium").enableClassInfo().scan()) {
            result.getAllClasses().forEach(classInfo -> {
                try {
                    hints.reflection().registerType(classInfo.loadClass(), MemberCategory.values());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
        }
    }

    private static void registerJeletaskHints(RuntimeHints hints) {
        hints.resources().registerPattern("test-config.json");
        hints.resources().registerPattern("haconfig/*");

        hints.reflection().registerType(Config.class, MemberCategory.values());
        hints.reflection().registerType(Entity.class, MemberCategory.values());
        hints.reflection().registerType(HAObject.class, MemberCategory.values());
        hints.reflection().registerType(Entity.Attributes.class, MemberCategory.values());
    }

    //Needed for now because otherwise the test containers library does not create a network alias for mqtt
    private static void registerTestContainersHints(RuntimeHints hints) {
        hints.resources().registerPattern("org/testcontainers/**");

        try (var result = new ClassGraph().acceptPackages("org.testcontainers", "com.codeborne").enableClassInfo().scan()) {
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
