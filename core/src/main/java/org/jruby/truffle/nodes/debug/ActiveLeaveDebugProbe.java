/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.debug;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

public abstract class ActiveLeaveDebugProbe extends RubyProbe {

    private final Assumption activeAssumption;
    private final RubyProc proc;
    @Child protected CallNode callNode;

    private final BranchProfile profile = new BranchProfile();

    public ActiveLeaveDebugProbe(RubyContext context, Assumption activeAssumption, RubyProc proc) {
        super(context, false);
        this.activeAssumption = activeAssumption;
        this.proc = proc;
        callNode = Truffle.getRuntime().createCallNode(proc.getMethod().getCallTarget());
    }

    @Override
    public void leave(Node astNode, VirtualFrame frame, Object result) {
        profile.enter();

        try {
            activeAssumption.check();
        } catch (InvalidAssumptionException e) {
            replace(createInactive());
            return;
        }

        Object[] internalArguments = RubyArguments.create(1);
        RubyArguments.setDeclarationFrame(internalArguments, proc.getMethod().getDeclarationFrame());
        RubyArguments.setSelf(internalArguments, NilPlaceholder.INSTANCE);
        RubyArguments.setUserArgument(internalArguments, 0, result);
        final RubyArguments arguments = new RubyArguments(internalArguments);
        callNode.call(frame.pack(), arguments);
    }

    protected abstract InactiveLeaveDebugProbe createInactive();

}
