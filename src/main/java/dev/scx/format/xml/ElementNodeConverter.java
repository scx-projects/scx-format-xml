package dev.scx.format.xml;

import dev.scx.format.xml.element.Element;
import dev.scx.format.xml.element.TagElement;
import dev.scx.node.IntNode;
import dev.scx.node.Node;

// todo 采取什么规则 ?
public class ElementNodeConverter {

    public static Node elementToNode(Element element) {

        return new IntNode(0);
    }

    public static TagElement nodeToElement(Node node) {

        return new TagElement("root");
    }

}
