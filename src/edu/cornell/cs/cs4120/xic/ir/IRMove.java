package edu.cornell.cs.cs4120.xic.ir;

import java.io.StringWriter;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import edu.cornell.cs.cs4120.xic.ir.visit.AggregateVisitor;
import edu.cornell.cs.cs4120.xic.ir.visit.IRVisitor;
import zr54.assembly.OpTarget;
import zr54.typechecker.FuncSymbolTable;

/**
 * An intermediate representation for a move statement
 * MOVE(target, expr)
 */
public class IRMove extends IRStmt {
    private IRExpr target;
    private IRExpr expr;

    /**
     *
     * @param target the destination of this move
     * @param expr the expression whose value is to be moved
     */
    public IRMove(IRExpr target, IRExpr expr) {
    	super();
        this.target = target;
        this.expr = expr;
        this.children.add(target);
        this.children.add(expr);
    }
    
    public void updateChildren() {
    	this.target = (IRExpr) this.children.get(0);
    	this.expr = (IRExpr) this.children.get(1);
    }

    public IRExpr target() {
        return target;
    }

    public IRExpr expr() {
        return expr;
    }

    @Override
    public String label() {
        return "MOVE";
    }

    @Override
    public IRNode visitChildren(IRVisitor v) {
        IRExpr target = (IRExpr) v.visit(this, this.target);
        IRExpr expr = (IRExpr) v.visit(this, this.expr);

        if (target != this.target || expr != this.expr)
            return new IRMove(target, expr);

        return this;
    }

    @Override
    public <T> T aggregateChildren(AggregateVisitor<T> v) {
        T result = v.unit();
        result = v.bind(result, v.visit(target));
        result = v.bind(result, v.visit(expr));
        return result;
    }

    @Override
    public void printSExp(SExpPrinter p) {
        p.startList();
        p.printAtom("MOVE");
        target.printSExp(p);
        expr.printSExp(p);
        p.endList();
    }
    
    /**
     * Do constant folding. If any children can be folded, replace it with a IRConst node.
     * @return if this node can be folded into a constant, return the IRConst node
     * 		   otherwise return null
     */
    @Override 
    public IRConst doConstFolding() {
    	IRConst result = target.doConstFolding();
    	if(result != null) {
    		target = result;
    		children.set(0, result);
    	}
    		
    	result = expr.doConstFolding();
    	if(result != null) {
    		expr = result;
    		children.set(1, result);
    	}
    	return null;
    }

	@Override
	public OpTarget genAssem(StringWriter sw, IRFuncDecl f, FuncSymbolTable funcs) {
		// TODO Auto-generated method stub
		
		if(target instanceof IRMem) {
			IRMem memTarget = (IRMem) target;
			
			//(MOVE (MEM (ADD (XX XX) (CONST XX))) (XX XX))			
			if(memTarget.expr() instanceof IRBinOp
					&& ((IRBinOp) memTarget.expr()).opType() == IRBinOp.OpType.ADD 
					&& ((IRBinOp) memTarget.expr()).right() instanceof IRConst) {
				
				OpTarget addr = ((IRBinOp) memTarget.expr()).left().genAssem(sw, f, funcs);
				IRConst offset = (IRConst) ((IRBinOp) memTarget.expr()).right();
				
				
				if(expr instanceof IRConst) {	//(MOVE (MEM (ADD (XX XX) (CONST XX))) (CONST XX))
					sw.write("# MOVE CONST " + ((IRConst)expr).value() + " to MEM\n");
					sw.write("	movq	" + addr.getTarget(false) + ", %r11\n"
							+"	movq	$" + ((IRConst)expr).value() + ", " + offset.value() + "(%r11)\n");
				}
				else {	//(MOVE (MEM (ADD (XX XX) (CONST XX))) (XX XX))
					OpTarget src = expr.genAssem(sw, f, funcs);
					sw.write("# MOVE t" + src.num + " to MEM\n");
					sw.write("	movq	" + src.getTarget(false) + ", %r10\n"
							+"	movq	" + addr.getTarget(false) + ", %r11\n"
							+"	movq	%r10, " + offset.value() + "(%r11)\n");

				}
			}
			else {
				OpTarget src = expr.genAssem(sw, f, funcs);
				OpTarget addr = memTarget.expr().genAssem(sw, f, funcs);

				
				if(expr instanceof IRConst) {
					sw.write("# MOVE CONST" + ((IRConst) expr).value() + " to MEM\n");
					sw.write("	movq	" + addr.getTarget(false) + ", %r11\n"
							+"	movq	$" + ((IRConst) expr).value() + ", (%r11)\n");

				}
				else {
					if(src.type == OpTarget.TempType.TEMP && addr.type == OpTarget.TempType.TEMP) {
						sw.write("# MOVE from t" + src.num + " to (t" + addr.num + ")\n");
					}
					sw.write("	movq	" + src.getTarget(false) + ", %r10\n" 
							+"	movq	" + addr.getTarget(false) + ", %r11\n"
							+"	movq	%r10, (%r11)\n");
				}
			}
		}
		else {
			if(expr instanceof IRConst) {
				OpTarget dst = target.genAssem(sw, f, funcs);
				String d = dst.getTarget(true);
				sw.write("	movq	$" + ((IRConst)expr).value() + ", " + d + "\n");
			}
			else {
				OpTarget src = expr.genAssem(sw, f, funcs);
				OpTarget dst = target.genAssem(sw, f, funcs);
				String s = src.getTarget(false);
				String d = dst.getTarget(true);
				if(src.type == OpTarget.TempType.TEMP && dst.type == OpTarget.TempType.TEMP) {
					sw.write("# MOVE from t" + src.num + " to t" + dst.num + "\n");
				}

				if(s.contains("(")&&d.contains("(")){
					sw.write("	movq	" + s + ", %r10\n"
							+"	movq	%r10, " + d + "\n");
				}else{
					sw.write("	movq	" + s + ", " + d + "\n");
				}
			}
		}

		return operand;
	}
}
