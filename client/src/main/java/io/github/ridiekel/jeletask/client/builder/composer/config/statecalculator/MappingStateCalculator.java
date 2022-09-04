package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

import java.util.HashMap;
import java.util.Map;

public class MappingStateCalculator extends SimpleStateCalculator {
    private final Map<String, String> byName = new HashMap<>();
    private final Map<Integer, String> byNumber = new HashMap<>();

    public MappingStateCalculator(NumberConverter numberConverter, StateMapping... mappings) {
        super(numberConverter);
        this.register(mappings);
    }

    private void register(StateMapping... mappings) {
        for (StateMapping mapping : mappings) {
            if (mapping.getRead() != null) {
                this.byName.put(mapping.getName().toUpperCase(), String.valueOf(mapping.getWrite()));
                this.byNumber.put(mapping.getRead().intValue(), mapping.getName().toUpperCase());
                this.byNumber.put(mapping.getWrite().intValue(), mapping.getName().toUpperCase());
            }
        }
    }

    @Override
    public ComponentState convertGet(byte[] dataBytes) {
        return new ComponentState(this.byNumber.get(this.getNumberConverter().convert(dataBytes).intValue()));
    }

    @Override
    public byte[] convertSetState(ComponentState value) {
        return super.convertSetState(new ComponentState(this.byName.get(value.getState())));
    }

    @Override
    public boolean isValidState(ComponentState state) {
        return this.byName.containsKey(state.getState().toUpperCase());
    }

    @Override
    public ComponentState getDefaultState(ComponentSpec component) {
        return new ComponentState(this.byName.keySet().stream().findFirst().orElse(null));
    }
}
