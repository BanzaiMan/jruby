package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.parser.IRStaticScope;

import java.util.Map;
import org.jruby.ir.operands.ScopeModule;

public class DefineInstanceMethodInstr extends Instr implements FixedArityInstr {
    private final IRMethod method;
    private final IRScopeType targetScopeType;

    // SSS FIXME: Implicit self arg -- make explicit to not get screwed by inlining!
    public DefineInstanceMethodInstr(IRMethod method, IRScopeType targetScopeType) {
        super(Operation.DEF_INST_METH);
        this.method = method;
        this.targetScopeType = targetScopeType;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[]{ new ScopeModule(method) };
    }

    @Override
    public String toString() {
        return getOperation() + "(" + method.getName() + ", " + method.getFileName() + ", target:" + targetScopeType + ")";
    }

    public IRMethod getMethod() {
        return method;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineInstanceMethodInstr(method, targetScopeType);
    }

    // SSS FIXME: Go through this and DefineClassMethodInstr.interpret, clean up, extract common code
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.runtime;
        RubyModule clazz = IRRuntimeHelpers.findInstanceMethodContainer(context, currDynScope, self, targetScopeType);

        //if (clazz != context.getRubyClass()) {
        //    System.out.println("*** DING DING DING! *** For " + this + "; clazz: " + clazz + "; ruby module: " + context.getRubyClass());
        //}

        String     name  = method.getName();
        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, clazz, name, currVisibility);

        DynamicMethod newMethod = new InterpretedIRMethod(method, newVisibility, clazz);

        Helpers.addInstanceMethod(clazz, name, newMethod, currVisibility, context, runtime);

        return null; // unused; symbol is propagated
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineInstanceMethodInstr(this);
    }
}
