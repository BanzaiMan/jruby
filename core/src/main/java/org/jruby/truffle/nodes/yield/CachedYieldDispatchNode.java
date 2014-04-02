/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.yield;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.CallNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

/**
 * Dispatch to a known method which has been inlined.
 */
public class CachedYieldDispatchNode extends YieldDispatchNode {

    private final RubyProc block;
    @Child protected CallNode callNode;

    public CachedYieldDispatchNode(RubyContext context, SourceSection sourceSection, RubyProc block) {
        super(context, sourceSection);
        this.block = block;

        callNode = Truffle.getRuntime().createCallNode(block.getMethod().getImplementation().getCallTarget());

        // Always try to split and inline blocks, they look like inline code so we'll treat them exactly like that

        if (callNode.isSplittable()) {
            callNode.split();
        }

        if (callNode.isInlinable()) {
            callNode.inline();
        }
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyProc block, Object[] argumentsObjects) {
        if (block.getMethod().getImplementation().getCallTarget() != callNode.getCallTarget()) {
            CompilerDirectives.transferToInterpreter();

            // TODO(CS): at the moment we just go back to uninit, which may cause loops

            getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, "uninitialized yield - may run in a loop");

            final UninitializedYieldDispatchNode dispatch = new UninitializedYieldDispatchNode(getContext(), getSourceSection());
            replace(dispatch);
            return dispatch.dispatch(frame, block, argumentsObjects);
        }

        final RubyArguments arguments = new RubyArguments(block.getMethod().getImplementation().getDeclarationFrame(), block.getSelf(), block.getBlock(), argumentsObjects);
        return callNode.call(frame.pack(), arguments);
    }
}
