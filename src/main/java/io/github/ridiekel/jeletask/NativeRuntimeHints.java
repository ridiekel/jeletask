package io.github.ridiekel.jeletask;

import io.github.ridiekel.jeletask.client.spec.*;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.*;
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
    }

    private static void registerJeletaskHints(RuntimeHints hints) {
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
        hints.reflection().registerType(MqttProcessor.PingMesage.class, MemberCategory.values());
    }
}
