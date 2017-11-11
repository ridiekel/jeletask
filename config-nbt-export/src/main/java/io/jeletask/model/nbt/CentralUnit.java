package io.jeletask.model.nbt;

import io.jeletask.model.spec.CentralUnitType;
import io.jeletask.model.spec.ClientConfigSpec;
import io.jeletask.model.spec.ComponentSpec;
import io.jeletask.model.spec.Function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CentralUnit implements ClientConfigSpec {
    private static final Room NONE = new Room(-1, "No Room");

    private static final Map<String, CentralUnitType> CENTRAL_UNIT_TYPE_MAP = Map.of(
            "TDS 10010: MICROS", CentralUnitType.MICROS,
            "TDS 10012: MICROS+", CentralUnitType.MICROS_PLUS
    );

    private String principalSite;
    private String name;
    private String type;
    private String serialNumber;
    private String host;
    private int port;
    private String macAddress;

    private List<Room> rooms;

    private List<OutputInterface> outputInterfaces;
    private List<InputInterface> inputInterfaces;

    private List<ComponentSupport> components;
    private Map<Function, List<ComponentSupport>> componentsByFunction;

    public String getPrincipalSite() {
        return this.principalSite;
    }

    public void setPrincipalSite(String principalSite) {
        this.principalSite = principalSite;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public CentralUnitType getCentralUnitType() {
        return CENTRAL_UNIT_TYPE_MAP.get(this.getType());
    }

    public String getSerialNumber() {
        return this.serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @Override
    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    @Override
    public List<Room> getRooms() {
        if (this.rooms == null) {
            this.setRooms(new ArrayList<Room>());
            this.getRooms().add(NONE);
        }
        return this.rooms;
    }

    private void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public List<ComponentSupport> getComponents() {
        if (this.components == null) {
            this.setComponents(new ArrayList<ComponentSupport>());
        }
        return this.components;
    }

    private void setComponents(List<ComponentSupport> components) {
        this.components = components;
    }

    public List<OutputInterface> getOutputInterfaces() {
        if (this.outputInterfaces == null) {
            this.setOutputInterfaces(new ArrayList<OutputInterface>());
        }
        return this.outputInterfaces;
    }

    private void setOutputInterfaces(List<OutputInterface> outputInterfaces) {
        this.outputInterfaces = outputInterfaces;
    }

    public List<InputInterface> getInputInterfaces() {
        if (this.inputInterfaces == null) {
            this.setInputInterfaces(new ArrayList<InputInterface>());
        }
        return this.inputInterfaces;
    }

    private void setInputInterfaces(List<InputInterface> inputInterfaces) {
        this.inputInterfaces = inputInterfaces;
    }


    public InputInterface findInputInterface(String autobusId, String autobusType, String autobusNumber) {
        return this.getInputInterfaceMap().get(InterfaceSupport.getCompleteAutobusId(autobusId, autobusType, autobusNumber));
    }

    public OutputInterface findOutputInterface(String autobusId, String autobusType, String autobusNumber) {
        return this.getOutputInterfaceMap().get(InterfaceSupport.getCompleteAutobusId(autobusId, autobusType, autobusNumber));
    }

    public Room findRoom(String name) {
        Room room = this.getRoomMap().get(name);
        if (room == null) {
            room = NONE;
        }
        return room;
    }

    @Override
    public ComponentSpec getComponent(Function function, int number) {
        return this.getComponentMap().get(this.getIndex(function, number));
    }

    @Override
    public List<ComponentSupport> getComponents(final Function function) {
        Map<Function, List<ComponentSupport>> map = this.getComponentsByFunction();
        List<ComponentSupport> componentSupports = map.get(function);
        if (componentSupports == null) {
            componentSupports = this.getComponents().stream().filter(i -> i.getFunction() == function).collect(Collectors.toList());
            map.put(function, componentSupports);
        }
        return componentSupports;
    }

    public Map<Function, List<ComponentSupport>> getComponentsByFunction() {
        if (this.componentsByFunction == null) {
            this.setComponentsByFunction(new HashMap<>());
        }
        return this.componentsByFunction;
    }

    private void setComponentsByFunction(Map<Function, List<ComponentSupport>> componentsByFunction) {
        this.componentsByFunction = componentsByFunction;
    }

    private transient Map<String, InputInterface> inputInterfaceMap;

    private Map<String, InputInterface> getInputInterfaceMap() {
        if (this.inputInterfaceMap == null) {
            this.inputInterfaceMap = this.convertInterfacesToMap(this.getInputInterfaces());
        }
        return this.inputInterfaceMap;
    }

    private transient Map<String, OutputInterface> outputInterfaceMap;

    private Map<String, OutputInterface> getOutputInterfaceMap() {
        if (this.outputInterfaceMap == null) {
            this.outputInterfaceMap = this.convertInterfacesToMap(this.getOutputInterfaces());
        }
        return this.outputInterfaceMap;
    }

    private <T extends InterfaceSupport> Map<String, T> convertInterfacesToMap(Collection<T> interfaces) {
        return interfaces.stream().collect(Collectors.toMap(InterfaceSupport::getCompleteAutobusId, java.util.function.Function.identity()));
    }

    private transient Map<String, Room> roomMap;

    private Map<String, Room> getRoomMap() {
        if (this.roomMap == null) {
            this.roomMap = this.convertRoomsToMap(this.getRooms());
        }
        return this.roomMap;
    }

    private Map<String, Room> convertRoomsToMap(Collection<Room> rooms) {
        return rooms.stream().collect(Collectors.toMap(Room::getName, java.util.function.Function.identity()));
    }
    private transient Map<String, ComponentSupport> componentMap;

    private Map<String, ComponentSupport> getComponentMap() {
        if (this.componentMap == null || this.componentMap.size() != this.getComponents().size()) {
            this.componentMap = this.convertComponentsToMap(this.getComponents());
        }
        return this.componentMap;
    }

    private Map<String, ComponentSupport> convertComponentsToMap(Collection<ComponentSupport> components) {
        return components.stream().collect(Collectors.toMap(c -> CentralUnit.this.getIndex(c.getFunction(), c.getId()), java.util.function.Function.identity()));
    }

    private String getIndex(Function function, int id) {
        return function + ":" + id;
    }

    @Override
    public List<? extends ComponentSpec> getAllComponents() {
        return this.getComponents();
    }
}