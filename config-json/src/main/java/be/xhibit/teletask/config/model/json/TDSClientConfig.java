package be.xhibit.teletask.config.model.json;

import be.xhibit.teletask.model.spec.CentralUnitType;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.ComponentSpec;
import be.xhibit.teletask.model.spec.Function;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * POJO representation of the TDS config JSON file.
 */
//@JsonIgnoreProperties(ignoreUnknown = true)
public class TDSClientConfig implements ClientConfigSpec {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TDSClientConfig.class);

    private String host;
    private int port;
    private Map<Function, List<TDSComponent>> componentsTypes;
    private List<Room> rooms;
    private List<TDSComponent> allComponents;

    /**
     * Default constructor.
     */
    private TDSClientConfig() {
    }

    public TDSClientConfig(String host, int port) {
        this.host = host;
        this.port = port;
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


    public Map<Function, List<TDSComponent>> getComponentsTypes() {
        return this.componentsTypes;
    }

    public void setComponentsTypes(Map<Function, List<TDSComponent>> componentsTypes) {
        this.componentsTypes = componentsTypes;
    }

    public List<Room> getRooms() {
        return this.rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    @Override
    public CentralUnitType getCentralUnitType() {
        //TODO: Get from json config file
        return CentralUnitType.MICROS;
    }

// ================================ HELPER METHODS

    @Override
    public TDSComponent getComponent(Function function, int number) {
        TDSComponent returnValue = null;

        List<TDSComponent> components = this.componentsTypes.get(function);

        //components.get()  ///TODO: refactor: access by index not OK, should be based on number, therefore iterate, or store as HashMap.
        for (TDSComponent component : components) {
            if (component.getNumber() == number) {
                component.setFunction(function);
                returnValue = component;
                break;
            }
        }

        return returnValue;
    }

    @Override
    public List<? extends ComponentSpec> getComponents(Function function) {
        return this.componentsTypes.get(function);
    }

    @Override
    public List<? extends ComponentSpec> getAllComponents() {
        if (this.allComponents == null) {
            this.allComponents = new ArrayList<>();
            for (List<TDSComponent> components : componentsTypes.values()) {
                this.allComponents.addAll(components);
            }
        }
        return this.allComponents;
    }

    public List<Room> getRooms(int level) {

        //components.get()  ///TODO: refactor: access by index not OK, should be based on number, therefore iterate, or store as HashMap.
        List<Room> roomsOnLevel = new ArrayList<>();
        for (Room room : this.rooms) {
            if (room.getLevel() == level) {
                //returnValue = room;
                roomsOnLevel.add(room);
            }
        }

        return roomsOnLevel;
    }

    /**
     * Convenience method.
     * Until a better Jackson ObjectMapper implementation, loop through all rooms and replace component number by actual object reference.
     */
    public void initRooms() {
        for (Room room : this.rooms) {
            Set<Function> functions = room.getComponentTypes().keySet();
            for (Function function : functions) {
                List<Integer> componentTypes = room.getComponentTypes().get(function);
                for (Integer componentNumber : componentTypes) {
                    TDSComponent component = this.getComponent(function, componentNumber);
                    Map<Function, List<TDSComponent>> components = room.getComponents();
                    List<TDSComponent> tdsComponents = components.get(function);
                    if (tdsComponents == null || tdsComponents.size() <= 0) {
                        tdsComponents = new ArrayList<>();
                        components.put(function, tdsComponents);
                    }
                    tdsComponents.add(component);
                }
            }
        }
    }

    public static TDSClientConfig read(InputStream jsonData) throws IOException {

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        //convert json string to object
        TDSClientConfig clientConfig = objectMapper.readValue(jsonData, TDSClientConfig.class);
        LOG.debug("JSON Config loaded: TDS HOST: {}:{}", clientConfig.getHost(), clientConfig.getPort());

        // until a better Jackson ObjectMapper implementation, loop through all rooms and replace component number by actual object reference
        clientConfig.initRooms();

        LOG.debug("TDSClientConfig initialized.");

        return clientConfig;
    }
}
