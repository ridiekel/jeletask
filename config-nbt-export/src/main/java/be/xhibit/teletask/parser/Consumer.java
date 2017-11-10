package be.xhibit.teletask.parser;

public interface Consumer {
    void principalSite(String value);

    void name(String value);

    void type(String value);

    void serialNumber(String value);

    void ipAddress(String value);

    void portNumber(String value);

    void macAddress(String value);

    void room(String id, String name);

    void outputInterface(String autobusId, String autobusType, String autobusNumber, String type, String name);

    void inputInterface(String autobusId, String autobusType, String autobusNumber, String name);

    void relay(String id, String roomName, String type, String description);

    void dimmer(String id, String roomName, String type, String description);

    void input(String autobusId, String autobusType, String autobusNumber, String id, String name, String shortActionType, String shortActionId, String longActionType, String longActionId);

    void localMood(String id, String roomName, String type, String description);

    void motor(String id, String roomName, String type, String description);

    void generalMood(String id, String roomName, String type, String description);

    void condition(String id, String description);

    void sensor(String id, String type, String description);
}