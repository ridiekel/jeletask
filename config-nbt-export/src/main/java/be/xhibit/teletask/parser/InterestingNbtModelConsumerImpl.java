package be.xhibit.teletask.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterestingNbtModelConsumerImpl extends FullNbtModelConsumerImpl {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(InterestingNbtModelConsumerImpl.class);

    @Override
    public void input(String autobusId, String autobusType, String autobusNumber, String id, String name, String shortActionType, String shortActionId, String longActionType, String longActionId) {
//        super.input(autobusId, autobusType, autobusNumber, id, name, shortActionType, shortActionId, longActionType, longActionId);
    }

    @Override
    public void relay(String id, String roomName, String type, String description) {
        super.relay(id, roomName, type, description);
    }

    @Override
    public void inputInterface(String autobusId, String autobusType, String autobusNumber, String name) {
//        super.inputInterface(autobusId, autobusType, autobusNumber, name);
    }

    @Override
    public void outputInterface(String autobusId, String autobusType, String autobusNumber, String type, String name) {
//        super.outputInterface(autobusId, autobusType, autobusNumber, type, name);
    }

    @Override
    public void localMood(String id, String roomName, String type, String description) {
        super.localMood(id, roomName, type, description);
    }

    @Override
    public void dimmer(String id, String roomName, String type, String description) {
        super.dimmer(id, roomName, type, description);
    }

    @Override
    public void generalMood(String id, String roomName, String type, String description) {
        super.generalMood(id, roomName, type, description);
    }

    @Override
    public void condition(String id, String description) {
        super.condition(id, description);
    }

    @Override
    public void room(String id, String name) {
        super.room(id, name);
    }

    @Override
    public void motor(String id, String roomName, String type, String description) {
        super.motor(id, roomName, type, description);
    }

    @Override
    public void macAddress(String value) {
        super.macAddress(value);
    }

    @Override
    public void portNumber(String value) {
        super.portNumber(value);
    }

    @Override
    public void ipAddress(String value) {
        super.ipAddress(value);
    }

    @Override
    public void serialNumber(String value) {
        super.serialNumber(value);
    }

    @Override
    public void type(String value) {
        super.type(value);
    }

    @Override
    public void name(String value) {
        super.name(value);
    }

    @Override
    public void principalSite(String value) {
        super.principalSite(value);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}