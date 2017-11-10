package be.xhibit.teletask.model.nbt;

import java.util.ArrayList;
import java.util.List;

public class InputInterface extends InterfaceSupport {
    private List<Input> inputs;

    public InputInterface(String autobusId, String autobusType, String autobusNumber, String name) {
        super(autobusId, autobusType, autobusNumber, name);
    }

    public List<Input> getInputs() {
        if (this.inputs == null) {
            this.setInputs(new ArrayList<Input>());
        }
        return this.inputs;
    }

    private void setInputs(List<Input> inputs) {
        this.inputs = inputs;
    }
}
