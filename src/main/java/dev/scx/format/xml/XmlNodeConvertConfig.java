package dev.scx.format.xml;

/// XmlNodeConvertConfig (本质上是 具备 setter 的 XmlNodeConvertOptions)
///
/// @author scx567888
public final class XmlNodeConvertConfig implements XmlNodeConvertOptions {

    /// 最大嵌套深度
    private int maxNestingDepth;
    /// 最大子元素数量 (同时作用于属性和子元素)
    private int maxChildCount;
    /// 最大字符串长度 (同时作用于属性值和文本)
    private int maxStringLength;
    /// 根节点名称
    private String rootName;
    /// 匿名元素 名称
    private String itemName;

    private XmlNodeConvertConfig() {
        this.maxNestingDepth = 200; // 默认 200 既不会轻易栈溢出, 也足够应对 大多数 的情况
        this.maxChildCount = 5000;
        this.maxStringLength = 2000 * 10000;
        this.rootName = "root";
        this.itemName = "item";
    }

    public static XmlNodeConvertConfig of() {
        return new XmlNodeConvertConfig();
    }

    public static XmlNodeConvertConfig copyOf(XmlNodeConvertOptions options) {
        var config = new XmlNodeConvertConfig();
        config.maxNestingDepth(options.maxNestingDepth());
        config.maxChildCount(options.maxChildCount());
        config.maxStringLength(options.maxStringLength());
        config.rootName(options.rootName());
        config.itemName(options.itemName());
        return config;
    }

    @Override
    public int maxNestingDepth() {
        return maxNestingDepth;
    }

    public XmlNodeConvertConfig maxNestingDepth(int maxNestingDepth) {
        if (maxNestingDepth < 0) {
            throw new IllegalArgumentException("maxNestingDepth cannot < 0");
        }
        this.maxNestingDepth = maxNestingDepth;
        return this;
    }

    @Override
    public int maxChildCount() {
        return maxChildCount;
    }

    public XmlNodeConvertConfig maxChildCount(int maxChildCount) {
        if (maxChildCount < 0) {
            throw new IllegalArgumentException("maxChildCount cannot < 0");
        }
        this.maxChildCount = maxChildCount;
        return this;
    }

    @Override
    public int maxStringLength() {
        return maxStringLength;
    }

    public XmlNodeConvertConfig maxStringLength(int maxStringLength) {
        if (maxStringLength < 0) {
            throw new IllegalArgumentException("maxStringLength cannot < 0");
        }
        this.maxStringLength = maxStringLength;
        return this;
    }

    @Override
    public String rootName() {
        return rootName;
    }

    public XmlNodeConvertConfig rootName(String rootName) {
        if (rootName == null) {
            throw new NullPointerException("rootName cannot be null");
        }
        this.rootName = rootName;
        return this;
    }

    @Override
    public String itemName() {
        return itemName;
    }

    public XmlNodeConvertConfig itemName(String itemName) {
        if (itemName == null) {
            throw new NullPointerException("itemName cannot be null");
        }
        this.itemName = itemName;
        return this;
    }

}
