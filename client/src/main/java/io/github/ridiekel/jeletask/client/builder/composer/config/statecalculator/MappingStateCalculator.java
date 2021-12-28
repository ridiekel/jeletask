package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;

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
    public String convertGet(ComponentSpec component, byte[] value) {
        return this.byNumber.get(this.getNumberConverter().convert(value).intValue());
    }

    @Override
    public byte[] convertSet(ComponentSpec component, String value) {
        return super.convertSet(component, String.valueOf(this.byName.get(value)));
    }

    @Override
    public boolean isValidState(String state) {
        return this.byName.keySet().contains(state.toUpperCase());
    }

    @Override
    public String getDefaultState(ComponentSpec component) {
        return this.byName.keySet().stream().findFirst().orElse(null);
    }
}
