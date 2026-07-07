package dev.scx.format.xml.element;

/// TextElement
///
/// @author scx567888
public record TextElement(String text) implements Element {

    @Override
    public String toString() {
        return text;
    }

}
