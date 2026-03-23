package org.sokybot.packetsniffer.struct;

public class StructField {
    private String name;
    private String type;
    private int offset;
    private int length;
    private String comment;
    private String endian = "little";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getEndian() {
        return endian;
    }

    public void setEndian(String endian) {
        this.endian = endian;
    }
}
