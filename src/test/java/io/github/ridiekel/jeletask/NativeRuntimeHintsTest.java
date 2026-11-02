package io.github.ridiekel.jeletask;

import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.List;

// The scan reads .class files, and a native image has none
@DisabledInNativeImage
class NativeRuntimeHintsTest {
    private static final String BASE_PACKAGE = "io.github.ridiekel.jeletask";

    private static List<Class<?>> stateImplementations() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                // Also the abstract ones: Jackson needs those as well
                return beanDefinition.getMetadata().isIndependent();
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(State.class));
        return scanner.findCandidateComponents(BASE_PACKAGE).stream()
                .<Class<?>>map(definition -> ClassUtils.resolveClassName(definition.getBeanClassName(), NativeRuntimeHintsTest.class.getClassLoader()))
                // A state that only exists in a test is never part of the native image
                .filter(type -> type.getProtectionDomain().getCodeSource().equals(State.class.getProtectionDomain().getCodeSource()))
                .toList();
    }

    @Test
    void theScanFindsTheStates() {
        // Without this a broken scan would make the test below pass on nothing
        Assertions.assertThat(stateImplementations()).contains(State.class, MotorState.class).hasSizeGreaterThanOrEqualTo(15);
    }

    @Test
    void everyStateIsRegisteredForReflection() {
        RuntimeHints hints = new RuntimeHints();
        new NativeRuntimeHints().registerHints(hints, NativeRuntimeHintsTest.class.getClassLoader());

        Assertions.assertThat(stateImplementations())
                .as("Register these in NativeRuntimeHints: Jackson reads and writes them by reflection in the native image")
                .allSatisfy(type -> Assertions.assertThat(RuntimeHintsPredicates.reflection().onType(type).withMemberCategories(MemberCategory.values()))
                        .as(type.getName())
                        .accepts(hints));
    }
}
