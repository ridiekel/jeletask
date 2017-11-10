package be.xhibit.teletask.model.nbt;

public abstract class InterfaceSupport {
    private final String autobusId;
    private final String autobusType;
    private final String autobusNumber;
    private final String name;

    public InterfaceSupport(String autobusId, String autobusType, String autobusNumber, String name) {
        this.autobusId = autobusId;
        this.autobusType = autobusType;
        this.autobusNumber = autobusNumber;
        this.name = name;
    }

    public String getAutobusId() {
        return this.autobusId;
    }

    public String getAutobusNumber() {
        return this.autobusNumber;
    }

    public String getName() {
        return this.name;
    }

    public String getAutobusType() {
        return this.autobusType;
    }

    public String getCompleteAutobusId() {
        return getCompleteAutobusId(this.getAutobusId(), this.getAutobusType(), this.getAutobusNumber());
    }

    public static String getCompleteAutobusId(String autobusId, String autobusType, String autobusNumber) {
        return autobusId + "-" + autobusType + autobusNumber;
    }
}
