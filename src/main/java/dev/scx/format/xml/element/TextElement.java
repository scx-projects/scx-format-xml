package dev.scx.format.xml.element;

/// TextElement
///
/// @author scx567888
/// @version 0.0.1
public record TextElement(String text) implements Element {

    @Override
    public String toString() {
        return text;
    }

}
