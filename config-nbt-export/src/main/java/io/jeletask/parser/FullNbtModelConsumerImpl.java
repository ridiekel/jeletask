package io.jeletask.parser;

import io.jeletask.model.nbt.CentralUnit;
import io.jeletask.model.nbt.Condition;
import io.jeletask.model.nbt.Dimmer;
import io.jeletask.model.nbt.GeneralMood;
import io.jeletask.model.nbt.Input;
import io.jeletask.model.nbt.InputInterface;
import io.jeletask.model.nbt.LocalMood;
import io.jeletask.model.nbt.Motor;
import io.jeletask.model.nbt.OutputInterface;
import io.jeletask.model.nbt.Relay;
import io.jeletask.model.nbt.Room;
import io.jeletask.model.nbt.Sensor;
import io.jeletask.model.spec.ComponentSpec;
import io.jeletask.model.spec.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FullNbtModelConsumerImpl implements Consumer {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FullNbtModelConsumerImpl.class);

    private final CentralUnit centralUnit;

    public FullNbtModelConsumerImpl() {
        this.centralUnit = new CentralUnit();
        Room room = new Room(-2, "Conditions");
        this.getCentralUnit().getRooms().add(room);
        room = new Room(-3, "Sensors");
        this.getCentralUnit().getRooms().add(room);
    }

    @Override
    public void principalSite(String value) {
        this.getLogger().debug("principalSite: {}", value);
        this.getCentralUnit().setPrincipalSite(value);
    }

    @Override
    public void name(String value) {
        this.getLogger().debug("name: {}", value);
        this.getCentralUnit().setName(value);
    }

    @Override
    public void type(String value) {
        this.getLogger().debug("type: {}", value);
        this.getCentralUnit().setType(value);
    }

    @Override
    public void serialNumber(String value) {
        this.getLogger().debug("serialNumber: {}", value);
        this.getCentralUnit().setSerialNumber(value);
    }

    @Override
    public void ipAddress(String value) {
        this.getLogger().debug("ipAddress: {}", value);
        this.getCentralUnit().setHost(value);
    }

    @Override
    public void portNumber(String value) {
        this.getLogger().debug("portNumber: {}", value);
        this.getCentralUnit().setPort(Integer.valueOf(value));
    }

    @Override
    public void macAddress(String value) {
        this.getLogger().debug("macAddress: {}", value);
        this.getCentralUnit().setMacAddress(value);
    }

    @Override
    public void room(String id, String name) {
        this.getLogger().debug("room: {} - {}", id, name);
        this.getCentralUnit().getRooms().add(new Room(Integer.valueOf(id), name));
    }

    @Override
    public void outputInterface(String autobusId, String autobusType, String autobusNumber, String type, String name) {
        this.getLogger().debug("outputInterface: {}:{}:{} {} - {}", autobusId, autobusType, autobusNumber, type, name);
        this.getCentralUnit().getOutputInterfaces().add(new OutputInterface(autobusId, autobusType, autobusNumber, type, name));
    }

    @Override
    public void inputInterface(String autobusId, String autobusType, String autobusNumber, String name) {
        this.getLogger().debug("inputInterface: {}:{}:{} - {}", autobusId, autobusType, autobusNumber, name);
        this.getCentralUnit().getInputInterfaces().add(new InputInterface(autobusId, autobusType, autobusNumber, name));
    }

    @Override
    public void relay(String id, String roomName, String type, String description) {
        this.getLogger().debug("relay: {}:{} (Room {}) - {}", type, id, roomName, description);
        Room room = this.getCentralUnit().findRoom(roomName);
        Relay relay = new Relay(Integer.valueOf(id), room, type, description);
        room.getRelays().add(relay);
        this.getCentralUnit().getComponents().add(relay);
    }

    @Override
    public void dimmer(String id, String roomName, String type, String description) {
        this.getLogger().debug("dimmer: {}:{} (Room {}) - {}", type, id, roomName, description);
        Room room = this.getCentralUnit().findRoom(roomName);
        Dimmer dimmer = new Dimmer(Integer.valueOf(id), room, type, description);
        room.getDimmers().add(dimmer);
        this.getCentralUnit().getComponents().add(dimmer);
    }

    @Override
    public void motor(String id, String roomName, String type, String description) {
        this.getLogger().debug("motor: {}:{} (Room {}) - {}", type, id, roomName, description);
        Room room = this.getCentralUnit().findRoom(roomName);
        Motor motor = new Motor(Integer.valueOf(id), room, type, description);
        room.getMotors().add(motor);
        this.getCentralUnit().getComponents().add(motor);
    }

    @Override
    public void generalMood(String id, String roomName, String type, String description) {
        this.getLogger().debug("generalMood: {}:{} (Room {}) - {}", type, id, roomName, description);
        Room room = this.getCentralUnit().findRoom(roomName);
        GeneralMood generalMood = new GeneralMood(Integer.valueOf(id), room, type, description);
        room.getGeneralMoods().add(generalMood);
        this.getCentralUnit().getComponents().add(generalMood);
    }

    @Override
    public void condition(String id, String description) {
        this.getLogger().debug("condition: {} - {}", id, description);
        Room room = this.getCentralUnit().findRoom("Conditions");
        Condition condition = new Condition(Integer.valueOf(id), room, "CON", description);
        room.getConditions().add(condition);
        this.getCentralUnit().getComponents().add(condition);
    }

    @Override
    public void sensor(String id, String type, String description) {
        this.getLogger().debug("sensor: {} - {}", id, description);
        Room room = this.getCentralUnit().findRoom("Sensors");

        if (type.toUpperCase().contains("TEMPERATURE")) {
            type = "TEMPERATURE";
        } else if (type.toUpperCase().contains("LIGHT")) {
            type = "LIGHT";
        } else if (type.toUpperCase().contains("HUMIDITY")) {
            type = "HUMIDITY";
        }

        Sensor sensor = new Sensor(Integer.valueOf(id), room, type, description);
        room.getSensors().add(sensor);
        this.getCentralUnit().getComponents().add(sensor);
    }

    @Override
    public void input(String autobusId, String autobusType, String autobusNumber, String id, String name, String shortActionType, String shortActionId, String longActionType, String longActionId) {
        this.getLogger().debug("input: {}:{}:{} - {} - {} [Short: {}:{}], [Long: {}:{}]", autobusId, autobusType, autobusNumber, id, name, shortActionType, shortActionId, longActionType, longActionId);
        InputInterface inputInterface = this.getCentralUnit().findInputInterface(autobusId, autobusType, autobusNumber);

        inputInterface.getInputs().add(new Input(id, name, this.getComponent(shortActionType, shortActionId), this.getComponent(longActionType, longActionId)));
    }

    @Override
    public void localMood(String id, String roomName, String type, String description) {
        this.getLogger().debug("localMood: {}:{} (Room {}) - {}", type, id, roomName, description);
        Room room = this.getCentralUnit().findRoom(roomName);
        LocalMood localMood = new LocalMood(Integer.valueOf(id), room, type, description);
        room.getLocalMoods().add(localMood);
        this.getCentralUnit().getComponents().add(localMood);
    }

    private ComponentSpec getComponent(String actionType, String actionId) {
        ComponentSpec component = null;
        if (!isNullOrEmpty(actionType) && !isNullOrEmpty(actionId)) {
            component = this.getCentralUnit().getComponent(ACTION_MAPPING.get(actionType), Integer.valueOf(actionId));
        }
        return component;
    }

    public static final String EMPTY = "";

    public static boolean isNullOrEmpty(String target) {
        return target == null || EMPTY.equals(target);
    }

    private static final Map<String, Function> ACTION_MAPPING = Map.of(
            "REL", Function.RELAY,
            "LMD", Function.LOCMOOD
    );

    public CentralUnit getCentralUnit() {
        return this.centralUnit;
    }

    protected Logger getLogger() {
        return LOG;
    }
}