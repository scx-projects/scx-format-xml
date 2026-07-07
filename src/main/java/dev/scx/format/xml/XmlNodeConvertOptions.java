package dev.scx.format.xml;

import dev.scx.format.FormatNodeConvertOptions;

/// XmlNodeConvertOptions
///
/// @author scx567888
public interface XmlNodeConvertOptions extends FormatNodeConvertOptions {

    int maxNestingDepth();

    int maxChildCount();

    int maxStringLength();

    String rootName();

    String itemName();

}
