package be.xhibit.teletask.model.nbt;

public class OutputInterface extends InterfaceSupport {
    private final String type;

    public OutputInterface(String autobusId, String autobusType, String autobusNumber, String type, String name) {
        super(autobusId, autobusType, autobusNumber, name);
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
