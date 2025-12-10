package dev.scx.format.xml;

import dev.scx.format.xml.element.TagElement;

import java.util.Arrays;

/// TagElementStack
///
/// @author scx567888
/// @version 0.0.1
final class TagElementStack {

    private TagElement[] _stack;
    private int _top;
    private int _end;

    public TagElementStack() {

    }

    // Not used yet but useful for limits (fail at [some high depth])
    public int size() {
        return _top;
    }

    public void push(TagElement node) {
        if (_top < _end) {
            _stack[_top++] = node; // lgtm [java/dereferenced-value-may-be-null]
            return;
        }
        if (_stack == null) {
            _end = 10;
            _stack = new TagElement[_end];
        } else {
            // grow by 50%, for most part
            _end += Math.min(4000, Math.max(20, _end >> 1));
            _stack = Arrays.copyOf(_stack, _end);
        }
        _stack[_top++] = node;
    }

    public TagElement popOrNull() {
        if (_top == 0) {
            return null;
        }
        // note: could clean up stack but due to usage pattern, should not make
        // any difference -- all nodes joined during and after construction and
        // after construction the whole stack is discarded
        return _stack[--_top];
    }

}
