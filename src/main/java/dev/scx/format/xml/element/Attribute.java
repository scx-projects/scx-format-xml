package dev.scx.format.xml.element;

public record Attribute(String name, String value) {

    @Override
    public String toString() {
        return name + "=\"" + value + "\"";
    }

}
