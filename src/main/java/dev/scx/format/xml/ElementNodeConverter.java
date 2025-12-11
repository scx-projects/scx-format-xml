package dev.scx.format.xml;

import dev.scx.format.FormatToNodeException;
import dev.scx.format.NodeToFormatException;
import dev.scx.format.xml.element.Element;
import dev.scx.format.xml.element.TagElement;
import dev.scx.format.xml.element.TextElement;
import dev.scx.node.*;

/// 因为 XML <-> 通用对象 并不是完全语义兼容的,
/// 比如数组, 根节点, 空值等.
/// 这里 我们规定一些 转换规则.
///
/// @author scx567888
/// @version 0.0.1
public class ElementNodeConverter {

    private final String rootName;
    private final int maxNestingDepth;
    private final String itemName;

    public ElementNodeConverter(XmlNodeConverterOptions options) {
        this.rootName = options.rootName();
        this.maxNestingDepth = options.maxNestingDepth();
        this.itemName = options.itemName();
    }

    /// ### elementToNode 规则:
    ///
    /// 1. `<a></a>` -> `""`
    ///     没有文本和子元素 -> StringNode("")
    ///
    /// 2. `<a/>` -> `NULL`
    ///     没有文本和子元素 (自闭合标签) -> NULL
    ///
    /// 3. `<a>123</a>` -> `"123"`
    ///     只有文本时 -> StringNode
    ///
    /// 4. `<a><b>123</b></a>` -> `{"b": "123"}`
    ///     存在子元素 -> ObjectNode
    ///
    /// 5. `<a name="jack"></a>` 或 `<a name="jack" />` -> `{"name": "jack"}`
    ///     存在属性 (无论是否自闭合) -> ObjectNode
    ///
    /// 6. `<a name="jack"><age>18</age></a>` -> `{"name": "jack", "age": "18"}`
    ///     属性和子元素相同方式看待 -> ObjectNode
    ///
    /// 7. `<a>000<b>123</b></a>` -> `{"b": "123", "": "000"}`
    ///     同时存在子元素和单个文本 -> 将文本视为单个 StringNode, 并以 "" 为 key 合并到 ObjectNode 中
    ///
    /// 8. `<a>000<b>123</b>6666</a>` -> `{"b": "123", "": ["000", "6666"]}`
    ///     同时存在子元素和多个文本 -> 将多个文本视为 ArrayNode(StringNode[]) , 并以 "" 为 key 合并到 Object 中
    ///
    /// 9. `<a><b>123</b><b>456</b></a>` -> `{"b": ["123", "456"]}`
    ///     存在多个同名子元素 -> 合并子元素为 ArrayNode
    ///
    /// 10, `<a name="jack" name="rose"></a>` -> `{"name": ["jack", "rose"]}`
    ///     存在多个同名属性 -> 合并属性为 ArrayNode
    ///
    /// 11, `<a name="">  <b> 1 2 3 </b>   </a>` -> `{"b": " 1 2 3 ", "name": "" }`
    ///     所有的纯空白文本节点视为不存在, 但有内容则保留原始文本, 属性永远保留原始文本
    public Node elementToNode(Element element) {
        return _elementToNode(element, 1);
    }

    private Node _elementToNode(Element element, int currentDepth) {
        if (currentDepth > maxNestingDepth) {
            throw new FormatToNodeException("Nesting depth exceeds limit: " + maxNestingDepth);
        }
        if (element instanceof TagElement tagElement) {
            // 记录出现过的子元素和属性
            var elements = new ObjectNode();

            // 1, 处理当前元素的属性
            for (var attribute : tagElement.attributes()) {
                var name = attribute.name();
                var value = attribute.value();
                // 可能存在重名元素
                var oldChildNode = elements.get(name);
                if (oldChildNode == null) {
                    elements.put(name, new StringNode(value));
                    continue;
                }
                //我们默认尝试转换成 数组
                if (oldChildNode instanceof ArrayNode arrayNode) {
                    arrayNode.add(new StringNode(value));
                } else {
                    var arrayNode = new ArrayNode();
                    arrayNode.add(oldChildNode);
                    arrayNode.add(new StringNode(value));
                    elements.put(name, arrayNode);
                }
            }

            // 2, 判断是否是自闭合标签
            var emptyElement = tagElement.isEmpty() && tagElement.useSelfClosing();
            // 自闭合标签 无需处理内部元素 直接返回
            if (emptyElement) {
                if (elements.isEmpty()) {
                    return NullNode.NULL;
                } else {
                    return elements;
                }
            }

            // 记录出现过的文本
            var texts = new ArrayNode();

            for (var e : tagElement) {
                if (e instanceof TagElement tag) {
                    var name = tag.tagName();
                    var ele = _elementToNode(tag, currentDepth + 1);
                    // 可能存在重名元素
                    var oldChildNode = elements.get(name);
                    if (oldChildNode == null) {
                        elements.put(name, ele);
                        continue;
                    }
                    //我们默认尝试转换成 数组
                    if (oldChildNode instanceof ArrayNode arrayNode) {
                        arrayNode.add(ele);
                    } else {
                        var arrayNode = new ArrayNode();
                        arrayNode.add(oldChildNode);
                        arrayNode.add(ele);
                        elements.put(name, arrayNode);
                    }
                } else if (e instanceof TextElement textElement) {
                    // 遇到了文本 进行存储
                    var text = textElement.text();
                    texts.add(new StringNode(text));
                }
            }

            // 没有任何子元素
            if (elements.isEmpty()) {
                // 如果文本也是空的
                if (texts.isEmpty()) {
                    return new StringNode("");
                }
                // 如果只有一个文本节点
                if (texts.size() == 1) {
                    return texts.get(0);
                }
                // 有很多文本节点 (应该不会出现这种情况)
                return texts;
            }
            // 如果只有一个文本节点
            if (texts.size() == 1) {
                elements.put("", texts.get(0));
                return elements;
            } else if (texts.size() > 1) {
                // 如果又很多文本节点 以数组形式添加
                elements.put("", texts);
            }

            return elements;
        } else if (element instanceof TextElement textElement) {
            var text = textElement.text();
            return new StringNode(text);
        } else {
            throw new FormatToNodeException("Invalid element type");
        }
    }

    /// ### nodeToElement 规则
    ///
    /// 0. 跟标签默认 -> root, 没有上下文 key 可用的数组 默认 -> item.
    ///    支持外部配置, 防止某些情况下的冲突
    ///
    /// 1. `"123"` -> `<root>123</root>`
    ///    值类型 -> 标准标签
    ///
    /// 2. `NULL` -> `<root/>`
    ///    NULL -> 闭合标签
    ///
    /// 3. `{"a": 123}` -> `<root><a>123</a></root>`
    ///    对象类型 -> 嵌套标签
    ///
    /// 4. `{"a": [1, 2]}` -> `<root><a>1</a><a>2</a></root>``
    ///    数组 -> 尝试重复
    ///
    /// 5. `[1, 2]` -> `<root><item>1</item><item>2</item></root>`
    ///    数组没有可用的上文 key -> 使用 item
    ///
    /// 6. `[1, [2]]` -> `<root><item>1</item><item><item>2</item></item></root>`
    ///    嵌套数组不进行任何扁平化
    ///
    /// 7, `{"": 123}` -> `<root>123</root>`
    ///    key 为 "", 直接解包
    public Element nodeToElement(Node node) {
        // 顶级数组需要特殊处理
        var isRootArray = node instanceof ArrayNode;
        return _nodeToElement(node, rootName, isRootArray, 1);
    }

    private Element _nodeToElement(Node node, String key, boolean inArray, int currentDepth) {
        if (currentDepth > maxNestingDepth) {
            throw new NodeToFormatException("Nesting depth exceeds limit: " + maxNestingDepth);
        }
        switch (node) {
            case NullNode _ -> {
                // 如果根节点本身就是 null, 直接返回自闭合标签
                return new TagElement(key, true);
            }
            case ValueNode valueNode -> {
                // "", 直接解包
                if (key.isEmpty()) {
                    return new TextElement(valueNode.asString());
                } else {
                    var el = new TagElement(key, false);
                    el.add(new TextElement(valueNode.asString()));
                    return el;
                }
            }
            case ObjectNode objectNode -> {
                var el = new TagElement(key, false);
                for (var e : objectNode) {
                    el.add(_nodeToElement(e.getValue(), e.getKey(), false, currentDepth + 1));
                }
                return el;
            }
            case ArrayNode arrayNode -> {
                if (inArray) {
                    var el = new TagElement(key, false);
                    for (var e : arrayNode) {
                        el.add(_nodeToElement(e, itemName, true, currentDepth + 1));
                    }
                    return el;
                } else {
                    var el = new TagElement(key, false);
                    for (var e : arrayNode) {
                        el.add(_nodeToElement(e, key, true, currentDepth + 1));
                    }
                    return el;
                }
            }
        }
    }

}
