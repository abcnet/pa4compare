package edu.cornell.cs.cs4120.xic.ir;

import java.io.StringWriter;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import edu.cornell.cs.cs4120.xic.ir.visit.AggregateVisitor;
import edu.cornell.cs.cs4120.xic.ir.visit.CheckCanonicalIRVisitor;
import edu.cornell.cs.cs4120.xic.ir.visit.IRVisitor;
import zr54.assembly.OpTarget;
import zr54.typechecker.FuncSymbolTable;

/**
 * An intermediate representation for a conditional transfer of control
 * CJUMP(expr, trueLabel, falseLabel)
 */
public class IRCJump extends IRStmt {
    private IRExpr expr;
    private String trueLabel, falseLabel;

    /**
     * Construct a CJUMP instruction with fall-through on false.
     * @param expr the condition for the jump
     * @param trueLabel the destination of the jump if {@code expr} evaluates
     *          to true
     */
    public IRCJump(IRExpr expr, String trueLabel) {
    	super();
    	this.expr = expr;
    	this.trueLabel = trueLabel;
    	this.falseLabel = null;
    	this.children.add(expr);
    }

    /**
     *
     * @param expr the condition for the jump
     * @param trueLabel the destination of the jump if {@code expr} evaluates
     *          to true
     * @param falseLabel the destination of the jump if {@code expr} evaluates
     *          to false
     */
    public IRCJump(IRExpr expr, String trueLabel, String falseLabel) {
        this.expr = expr;
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
        this.children.add(expr);
    }
    
    public void updateChildren() {
    	this.expr = (IRExpr) this.children.get(0);
    }

    public IRExpr expr() {
        return expr;
    }

    public String trueLabel() {
        return trueLabel;
    }

    public String falseLabel() {
        return falseLabel;
    }

    public boolean hasFalseLabel() {
        return falseLabel != null;
    }

    @Override
    public String label() {
        return "CJUMP";
    }

    @Override
    public IRNode visitChildren(IRVisitor v) {
        IRExpr expr = (IRExpr) v.visit(this, this.expr);

        if (expr != this.expr) return new IRCJump(expr, trueLabel, falseLabel);

        return this;
    }

    @Override
    public <T> T aggregateChildren(AggregateVisitor<T> v) {
        T result = v.unit();
        result = v.bind(result, v.visit(expr));
        return result;
    }

    @Override
    public boolean isCanonical(CheckCanonicalIRVisitor v) {
        return !hasFalseLabel();
    }

    @Override
    public void printSExp(SExpPrinter p) {
        p.startList();
        p.printAtom("CJUMP");
        expr.printSExp(p);
        p.printAtom(trueLabel);
        if (hasFalseLabel()) p.printAtom(falseLabel);
        p.endList();
    }
    
    /**
     * Do constant folding. If any children can be folded, replace it with a IRConst node.
     * @return if this node can be folded into a constant, return the IRConst node
     * 		   otherwise return null
     */
    @Override
    public IRConst doConstFolding() {
    	IRConst result = expr.doConstFolding();
    	if(result != null) {
    		expr = result;
    		children.set(0, result);
    	}
    	
    	return null;
    }

	@Override
	public OpTarget genAssem(StringWriter sw, IRFuncDecl f, FuncSymbolTable funcs) {
		// TODO Auto-generated method stub
		
		if(expr instanceof IRBinOp &&
			( (((IRBinOp) expr).opType() == IRBinOp.OpType.EQ) 
			||(((IRBinOp) expr).opType() == IRBinOp.OpType.NEQ)
			||(((IRBinOp) expr).opType() == IRBinOp.OpType.LT) 
			||(((IRBinOp) expr).opType() == IRBinOp.OpType.GT) 
			||(((IRBinOp) expr).opType() == IRBinOp.OpType.LEQ) 
			||(((IRBinOp) expr).opType() == IRBinOp.OpType.GEQ) )) {
			
			IRBinOp binExpr = (IRBinOp) expr;
			String jmpStr = "";
			switch(binExpr.opType()) {
			case EQ:
				jmpStr = "je";
				break;
			case NEQ:
				jmpStr = "jne";			
				break;
			case LT:
				jmpStr = "jl";
				break;
			case GT:
				jmpStr = "jg";
				break;
			case LEQ:
				jmpStr = "jle";
				break;
			case GEQ:
				jmpStr = "jge";
				break;
			}
			
			sw.write("# CJUMP BinOp " + jmpStr + "\n");
			OpTarget l = binExpr.left().genAssem(sw, f, funcs);
			OpTarget r = binExpr.right().genAssem(sw, f, funcs);
			sw.write("	movq	" + l.getTarget(false) + ", %rax\n"
					+"	cmpq	" + r.getTarget(false) + ", %rax\n");

			if(trueLabel != null) 
				sw.write("	" + jmpStr + "	" + trueLabel + "\n");
			if(falseLabel != null)
				sw.write("	jmp	" + falseLabel + "\n");
									
		}
		else {
			OpTarget cond = expr.genAssem(sw, f, funcs);
			if(cond.type == OpTarget.TempType.TEMP)
				sw.write("# CJUMP t" + cond.num + "\n");
			
			sw.write("	movq	" + cond.getTarget(false) + ", %rax\n"
					+"	testq	%rax, %rax\n"
					+"	jnz	" + trueLabel + "\n");
			if(falseLabel != null) {
				sw.write("	jmp	" + falseLabel + "\n");
			}
		}
		
		return operand;
	}
}
