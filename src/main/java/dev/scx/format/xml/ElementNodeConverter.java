package dev.scx.format.xml;

import dev.scx.format.FormatToNodeException;
import dev.scx.format.NodeToFormatException;
import dev.scx.format.xml.element.Element;
import dev.scx.format.xml.element.TagElement;
import dev.scx.format.xml.element.TextElement;
import dev.scx.node.*;

/// 因为 XML <-> Node 并不是完全语义兼容的,
/// 比如数组, 根节点, 空值等.
/// 这里 我们规定一些 转换规则.
///
/// @author scx567888
public final class ElementNodeConverter {

    private final String rootName;
    private final int maxNestingDepth;
    private final String itemName;

    public ElementNodeConverter(XmlNodeConvertOptions options) {
        this.rootName = options.rootName();
        this.maxNestingDepth = options.maxNestingDepth();
        this.itemName = options.itemName();
    }

    /// ### elementToNode 转换规则
    ///
    /// 本转换器将 XML Element 投影为通用数据模型 Node.
    /// 该映射面向 **data-centric XML** (数据型 XML),
    /// 不支持文档型 XML 的 mixed content (文本与子元素混排).
    ///
    /// 转换规则如下:
    ///
    /// 1. **纯空白文本忽略**
    ///    元素内部所有纯空白文本节点视为不存在;
    ///    属性值始终保留原始文本.
    ///
    /// 2. **禁止 mixed content**
    ///    若元素同时包含:
    ///      - 有效文本节点 (text.isBlank() == false)
    ///      - 以及属性或子元素
    ///    则视为 mixed content, 直接抛出 `FormatToNodeException`.
    ///
    /// 3. **文本元素**
    ///    若元素不包含任何结构内容（无属性、无子元素）,
    ///    则按文本元素处理:
    ///     - `<a/>`      -> `NullNode`
    ///     - `<a></a>`   -> `StringNode("")`
    ///     - `<a>123</a>` -> `StringNode("123")`
    ///
    /// 4. **对象元素**
    ///    若元素包含属性或子元素, 则转换为 `ObjectNode`.
    ///
    ///    - 属性作为对象字段
    ///    - 子元素也作为对象字段
    ///
    ///    示例:
    ///     - `<a name="jack"><age>18</age></a>` -> `{ "name": "jack", "age": "18" }`
    ///
    /// 5. **重复字段合并为数组**
    ///    若属性或子元素出现同名字段, 则自动合并为 `ArrayNode`.
    ///
    ///    示例:
    ///     - `<a><b>123</b><b>456</b></a>` -> `{ "b": ["123", "456"] }`
    ///
    ///     - `<a name="jack" name="rose"></a>` -> `{ "name": ["jack", "rose"] }`
    ///
    /// 6. **字段来源统一**
    ///    属性与子元素统一视为对象字段来源.
    ///    若属性名与子元素名相同, 按重复字段规则处理.
    ///
    /// 7. **数组允许异构元素**
    ///    由重复字段合并形成的 `ArrayNode` 允许包含不同类型元素.
    ///
    ///    示例:
    ///     - `<a><x>123</x><x/><x><y>1</y></x></a>` -> `{ "x": ["123", null, {"y": "1"}] }`
    ///
    /// ### 设计说明
    ///
    /// 该转换规则旨在提供稳定的 XML → Node 投影, 以支持后续的 Node → Java Object 绑定.
    ///
    /// 对于不符合 data-centric XML 结构的情况 (如 mixed content) ,
    /// 转换器将直接抛出异常, 而不是进行模糊或有损转换.
    public Node elementToNode(Element element) {
        return _elementToNode(element, 1);
    }

    private Node _elementToNode(Element element, int currentDepth) {
        if (currentDepth > maxNestingDepth) {
            throw new FormatToNodeException("Nesting depth exceeds limit: " + maxNestingDepth);
        }

        // 若输入本身就是 TextElement，则始终转换为 StringNode, 即使 (text.isBlank() == true).
        if (element instanceof TextElement textElement) {
            var text = textElement.text();
            return new StringNode(text);
        }

        // 这里因为密封类的原因, 必然是 TagElement 类型, 强转安全.
        var tagElement = (TagElement) element;

        // 是否存在属性.
        var hasAttr = tagElement.attributes().size() > 0;
        // 是否存在子元素 (特指 TagElement)
        var hasChild = false;
        // 所有文本节点, 这里我们合并为一个.
        var textBuilder = new StringBuilder();

        // 先扫描子内容，统计结构
        for (var child : tagElement) {
            switch (child) {
                case TagElement _ -> hasChild = true;
                case TextElement textElement -> {
                    var text = textElement.text();
                    // 忽略所有空白文本.
                    if (!text.isBlank()) {
                        textBuilder.append(text);
                    }
                }
            }
        }

        boolean hasText = textBuilder.length() > 0;

        // 混合内容: 文本 + 属性/子元素, 直接报错
        if (hasText && (hasAttr || hasChild)) {
            throw new FormatToNodeException("Mixed content is not supported");
        }

        // 文本元素
        if (!hasAttr && !hasChild) {
            if (!hasText && tagElement.useSelfClosing()) {
                return NullNode.NULL;
            }
            return new StringNode(textBuilder.toString());
        }

        // 对象元素
        var result = new ObjectNode();

        // 处理属性
        for (var attribute : tagElement.attributes()) {
            _mergeField(result, attribute.name(), new StringNode(attribute.value()));
        }

        // 处理子元素
        for (var child : tagElement) {
            if (child instanceof TagElement childTag) {
                var childNode = _elementToNode(childTag, currentDepth + 1);
                _mergeField(result, childTag.tagName(), childNode);
            }
        }

        return result;

    }

    private void _mergeField(ObjectNode objectNode, String name, Node value) {
        var oldValue = objectNode.get(name);
        if (oldValue == null) {
            objectNode.put(name, value);
            return;
        }

        if (oldValue instanceof ArrayNode arrayNode) {
            arrayNode.add(value);
        } else {
            var arrayNode = new ArrayNode();
            arrayNode.add(oldValue);
            arrayNode.add(value);
            objectNode.put(name, arrayNode);
        }
    }

    /// ### nodeToElement 转换规则
    ///
    /// 本转换器将通用数据模型 Node 投影为 XML Element.
    /// 该映射面向 **data-centric XML** (数据型 XML),
    /// 不生成 mixed content.
    ///
    /// 该规则与 `elementToNode` 的规则配套使用, 形成稳定的规范化映射.
    ///
    /// ### 转换规则
    ///
    /// 1. **根标签**
    ///    转换结果始终生成一个根标签.
    ///    根标签名称默认使用 `root`, 具体名称由外部选项提供.
    ///
    /// 2. **标量节点 (ValueNode)**
    ///    标量节点转换为普通文本标签.
    ///
    ///    示例:
    ///     - `"123"` -> `<root>123</root>`
    ///
    /// 3. **NullNode**
    ///    NullNode 转换为自闭合标签.
    ///
    ///    示例:
    ///     - `NULL` -> `<root/>`
    ///
    /// 4. **对象节点 (ObjectNode)**
    ///    ObjectNode 转换为普通标签.
    ///    每个字段转换为当前标签的子元素.
    ///
    ///    示例:
    ///     - `{ "a": 123 }` -> `<root><a>123</a></root>`
    ///
    /// 5. **对象字段的数组值**
    ///    若 ObjectNode 的字段值为 ArrayNode,
    ///    则该数组表示重复同名子元素.
    ///
    ///    数组中的每个元素分别转换为一个同名子元素,
    ///    不额外生成数组包装标签.
    ///
    ///    示例:
    ///     - `{ "a": [1, 2] }` -> `<root><a>1</a><a>2</a></root>`
    ///
    /// 6. **顶层数组 (ArrayNode)**
    ///    若根节点本身是 ArrayNode,
    ///    则数组中的每个元素分别转换为根标签的子元素.
    ///
    ///    因缺少字段名上下文,
    ///    默认使用 `item` 作为子元素标签名.
    ///
    ///    示例:
    ///     - `[1, 2]` -> `<root><item>1</item><item>2</item></root>`
    ///
    /// 7. **嵌套数组**
    ///    数组不进行扁平化.
    ///    若数组元素本身仍为数组, 则继续按同样规则递归转换.
    ///
    ///    示例:
    ///     - `[1, [2]]` -> `<root><item>1</item><item><item>2</item></item></root>`
    ///
    /// 8. **非法字段名**
    ///    空字符串键 `""` 无法生成标准 XML.
    ///    若 ObjectNode 中存在空字符串键, 直接抛出 `NodeToFormatException`.
    ///
    /// ### 设计说明
    ///
    /// 该规则提供 Node → XML 的稳定规范化映射,
    /// 并与 `elementToNode` 规则共同服务于 data-centric XML 与 Java Object 绑定.
    ///
    /// 映射原则:
    ///
    /// - ObjectNode 字段 → 子元素
    /// - ObjectNode 字段数组 → 重复同名子元素
    /// - 顶层 ArrayNode → 使用默认标签名 `item`
    ///
    /// 需要注意:
    ///
    /// - XML 属性来源信息不会被保留
    /// - XML 子元素顺序信息不会被保证
    ///
    /// 因此该映射为 **规范化映射 (canonical mapping)**,
    /// 而非原始 XML 结构的保真往返.
    public Element nodeToElement(Node node) {
        return _nodeToElement(node, rootName, 1);
    }

    private Element _nodeToElement(Node node, String key, int currentDepth) {
        if (currentDepth > maxNestingDepth) {
            throw new NodeToFormatException("Nesting depth exceeds limit: " + maxNestingDepth);
        }

        if (key.isEmpty()) {
            throw new NodeToFormatException("Empty field name is not allowed");
        }

        switch (node) {
            case NullNode _ -> {
                // 如果根节点本身就是 null, 直接返回自闭合标签
                return new TagElement(key, true);
            }
            case ValueNode valueNode -> {
                var el = new TagElement(key, false);
                el.add(new TextElement(valueNode.asString()));
                return el;
            }

            case ObjectNode objectNode -> {
                var el = new TagElement(key, false);

                for (var field : objectNode) {
                    var fieldName = field.getKey();
                    var fieldValue = field.getValue();

                    if (fieldName.isEmpty()) {
                        throw new NodeToFormatException("Empty field name is not allowed");
                    }
                    // 直接再这里处理 对象内嵌 数组
                    if (fieldValue instanceof ArrayNode arrayNode) {
                        for (var item : arrayNode) {
                            el.add(_nodeToElement(item, fieldName, currentDepth + 1));
                        }
                    } else {
                        el.add(_nodeToElement(fieldValue, fieldName, currentDepth + 1));
                    }
                }

                return el;
            }

            case ArrayNode arrayNode -> {
                var el = new TagElement(key, false);

                for (var item : arrayNode) {
                    el.add(_nodeToElement(item, itemName, currentDepth + 1));
                }

                return el;
            }
        }
    }

}
