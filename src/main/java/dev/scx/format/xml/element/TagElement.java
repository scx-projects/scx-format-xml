package dev.scx.format.xml.element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/// TagElement
///
/// @author scx567888
/// @version 0.0.1
public final class TagElement implements Element, Iterable<Element> {

    private final String tagName;
    private final boolean useSelfClosing;
    private final List<Attribute> attributes;
    private final List<Element> children;

    public TagElement(String tagName, boolean useSelfClosing) {
        this.tagName = tagName;
        this.useSelfClosing = useSelfClosing;
        this.attributes = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public String tagName() {
        return tagName;
    }

    public boolean useSelfClosing() {
        return useSelfClosing;
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

    @Override
    public String toString() {
        // 采用 XML 格式
        // 这里假设 TagElement 不存在自引用
        return toString0(0);
    }

    private String toString0(int indent) {
        // 采用 XML 格式
        // 这里假设 TagElement 不存在自引用
        var sb = new StringBuilder();

        // 缩进
        sb.append("    ".repeat(indent));

        // 开头
        sb.append("<").append(tagName);

        // 属性
        for (var attr : attributes) {
            sb.append(" ").append(attr.toString());
        }

        // 如果无子元素
        if (children.isEmpty()) {
            // 使用 自闭合
            if (useSelfClosing) {
                sb.append("/>");
            } else {
                sb.append("></").append(tagName).append(">");
            }
            return sb.toString();
        }

        sb.append(">");

        // 有子元素 → 换行并打印子节点
        sb.append("\n");

        for (var child : children) {
            if (child instanceof TagElement tag) {
                sb.append(tag.toString0(indent + 1));
                sb.append("\n");
            } else if (child instanceof TextElement text) {
                sb.append("    ".repeat(indent + 1))
                    .append(text.text())
                    .append("\n");
            }
        }

        // 尾部标签
        sb.append("    ".repeat(indent))
            .append("</").append(tagName).append(">");

        return sb.toString();
    }

}
