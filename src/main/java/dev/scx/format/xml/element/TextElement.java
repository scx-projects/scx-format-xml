package dev.scx.format.xml.element;

public record TextElement(String text) implements Element {

    @Override
    public String toString() {
        return text;
    }

}
