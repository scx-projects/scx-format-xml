package dev.scx.format.xml.element;

/// Attribute
///
/// @author scx567888
public record Attribute(String name, String value) {

    @Override
    public String toString() {
        return name + "=\"" + value + "\"";
    }

}
