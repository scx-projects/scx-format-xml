package dev.scx.format.xml.element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TagElement implements Element, Iterable<Element> {

    private final String tagName;

    private final List<Attribute> attributes;

    private final List<Element> children;

    public TagElement(String tagName) {
        this.tagName = tagName;
        this.attributes = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public String tagName() {
        return tagName;
    }

    //***************** children 相关方法 *******************

    public void add(Element element) {
        this.children.add(element);
    }

    public int size() {
        return children.size();
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    public void clear() {
        children.clear();
    }

    public List<Element> children() {
        return children;
    }

    @Override
    public Iterator<Element> iterator() {
        return children.iterator();
    }

    //***************** attributes 相关方法 *******************

    public void addAttribute(Attribute attribute) {
        this.attributes.add(attribute);
    }

    public void addAttribute(String name, String value) {
        attributes.add(new Attribute(name, value));
    }

    public int attributeSize() {
        return attributes.size();
    }

    public boolean isAttributeEmpty() {
        return attributes.isEmpty();
    }

    public void clearAttributes() {
        attributes.clear();
    }

    public List<Attribute> attributes() {
        return attributes;
    }

}
