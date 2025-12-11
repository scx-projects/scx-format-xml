package dev.scx.format.xml.element;

/// Attribute
///
/// @author scx567888
/// @version 0.0.1
public record Attribute(String name, String value) {

    @Override
    public String toString() {
        return name + "=\"" + value + "\"";
    }

}
