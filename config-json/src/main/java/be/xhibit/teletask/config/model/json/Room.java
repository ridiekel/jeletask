package be.xhibit.teletask.config.model.json;

import be.xhibit.teletask.model.spec.ComponentSpec;
import be.xhibit.teletask.model.spec.Function;
import be.xhibit.teletask.model.spec.RoomSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POJO representation of a Room.  Holds all components which are controllable in that specific room.
 */
public class Room implements RoomSpec {
    private int id;
    private int level;
    private String name;
    private Map<Function, List<TDSComponent>> components;
    private Map<Function, List<Integer>> componentTypes;

    @Override
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<? extends ComponentSpec> getRelays() {
        return this.getComponents().get(Function.RELAY);
    }

    public Map<Function, List<TDSComponent>> getComponents() {
        if (this.components == null) {
            this.components = new HashMap<>();
        }
        return this.components;
    }

    @Override
    public List<? extends ComponentSpec> getGeneralMoods() {
        return this.getComponents().get(Function.GENMOOD);
    }

    @Override
    public List<? extends ComponentSpec> getLocalMoods() {
        return this.getComponents().get(Function.LOCMOOD);
    }

    @Override
    public List<? extends ComponentSpec> getMotors() {
        return this.getComponents().get(Function.MOTOR);
    }

    @Override
    public List<? extends ComponentSpec> getDimmers() {
        return this.getComponents().get(Function.DIMMER);
    }

    @Override
    public List<? extends ComponentSpec> getSensors() {
        return this.getComponents().get(Function.SENSOR);
    }

    @Override
    public List<? extends ComponentSpec> getConditions() {
        return this.getComponents().get(Function.COND);
    }

    public void setComponents(Map<Function, List<TDSComponent>> components) {
        this.components = components;
    }

    public Map<Function, List<Integer>> getComponentTypes() {
        return this.componentTypes;
    }

    public void setComponentTypes(Map<Function, List<Integer>> componentTypes) {
        this.componentTypes = componentTypes;
    }
}
