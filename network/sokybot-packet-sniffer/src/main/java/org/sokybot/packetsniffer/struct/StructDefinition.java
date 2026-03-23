package org.sokybot.packetsniffer.struct;

import java.util.ArrayList;
import java.util.List;

public class StructDefinition {
    private int opcode;
    private List<StructField> fields = new ArrayList<>();

    public int getOpcode() {
        return opcode;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public List<StructField> getFields() {
        return fields;
    }

    public void setFields(List<StructField> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }
}
