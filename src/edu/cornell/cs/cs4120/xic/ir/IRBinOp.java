package edu.cornell.cs.cs4120.xic.ir;

import java.util.ArrayList;
import java.io.StringWriter;
import java.math.BigInteger;

import edu.cornell.cs.cs4120.util.InternalCompilerError;
import edu.cornell.cs.cs4120.util.SExpPrinter;
import edu.cornell.cs.cs4120.xic.ir.interpret.IRSimulator.Trap;
import edu.cornell.cs.cs4120.xic.ir.visit.AggregateVisitor;
import edu.cornell.cs.cs4120.xic.ir.visit.CheckConstFoldedIRVisitor;
import edu.cornell.cs.cs4120.xic.ir.visit.IRVisitor;
import zr54.assembly.OpTarget;
import zr54.typechecker.FuncSymbolTable;

/**
 * An intermediate representation for a binary operation
 * OP(left, right)
 */
public class IRBinOp extends IRExpr {

    /**
     * Binary operators
     */
    public enum OpType {
        ADD, SUB, MUL, HMUL, DIV, MOD, AND, OR, XOR, LSHIFT, RSHIFT, ARSHIFT,
        EQ, NEQ, LT, GT, LEQ, GEQ;

        @Override
        public String toString() {
            switch (this) {
            case ADD:
                return "ADD";
            case SUB:
                return "SUB";
            case MUL:
                return "MUL";
            case HMUL:
                return "HMUL";
            case DIV:
                return "DIV";
            case MOD:
                return "MOD";
            case AND:
                return "AND";
            case OR:
                return "OR";
            case XOR:
                return "XOR";
            case LSHIFT:
                return "LSHIFT";
            case RSHIFT:
                return "RSHIFT";
            case ARSHIFT:
                return "ARSHIFT";
            case EQ:
                return "EQ";
            case NEQ:
                return "NEQ";
            case LT:
                return "LT";
            case GT:
                return "GT";
            case LEQ:
                return "LEQ";
            case GEQ:
                return "GEQ";
            }
            throw new InternalCompilerError("Unknown op type");
        }
    };

    private OpType type;
    private IRExpr left, right;

    public IRBinOp(OpType type, IRExpr left, IRExpr right) {
    	super();
        this.type = type;
        this.left = left;
        this.right = right;
        this.children.add(left);
        this.children.add(right);
    }
    
    public void updateChildren() {
    	this.left = (IRExpr) this.children.get(0);
    	this.right = (IRExpr) this.children.get(1);
    }

    public OpType opType() {
        return type;
    }

    public IRExpr left() {
        return left;
    }

    public IRExpr right() {
        return right;
    }

    @Override
    public String label() {
        return type.toString();
    }

    @Override
    public IRNode visitChildren(IRVisitor v) {
        IRExpr left = (IRExpr) v.visit(this, this.left);
        IRExpr right = (IRExpr) v.visit(this, this.right);

        if (left != this.left || right != this.right)
            return new IRBinOp(type, left, right);

        return this;
    }

    @Override
    public <T> T aggregateChildren(AggregateVisitor<T> v) {
        T result = v.unit();
        result = v.bind(result, v.visit(left));
        result = v.bind(result, v.visit(right));
        return result;
    }

    @Override
    public boolean isConstFolded(CheckConstFoldedIRVisitor v) {
        return !isConstant();
    }

    @Override
    public boolean isConstant() {
        return left.isConstant() && right.isConstant();
    }

    @Override
    public void printSExp(SExpPrinter p) {
        p.startList();
        p.printAtom(type.toString());
        left.printSExp(p);
        right.printSExp(p);
        p.endList();
    }
    
    /**
     * Do constant folding. If any children can be folded, replace it with a IRConst node.
     * @return if this node can be folded into a constant, return the IRConst node
     * 		   otherwise return null
     */
    @Override
    public IRConst doConstFolding() {
    	IRConst lConst = left.doConstFolding();
    	IRConst rConst = right.doConstFolding();
    	
    	if(lConst != null) { 
    		this.left = lConst;
    		this.children.set(0, lConst);
    	}
    	if(rConst != null) {
    		this.right= rConst;
    		this.children.set(1, rConst);
    	}
    	
    	if(lConst == null || rConst == null)
    		return null;
    	else { //if both children are constant, this node is constant-foldable and we return the IRConst after folding
    		long l = lConst.value();
    		long r = rConst.value();
    		long result;
    		switch(this.opType()) {
            case ADD:
                result = l + r;
                break;
            case SUB:
                result = l - r;
                break;
            case MUL:
                result = l * r;
                break;
            case HMUL:
                result = BigInteger.valueOf(l)
                                   .multiply(BigInteger.valueOf(r))
                                   .shiftRight(64)
                                   .longValue();
                break;
            case DIV:
                if (r == 0) throw new Trap("Division by zero!");
                result = l / r;
                break;
            case MOD:
                if (r == 0) throw new Trap("Division by zero!");
                result = l % r;
                break;
            case AND:
                result = l & r;
                break;
            case OR:
                result = l | r;
                break;
            case XOR:
                result = l ^ r;
                break;
            case LSHIFT:
                result = l << r;
                break;
            case RSHIFT:
                result = l >>> r;
                break;
            case ARSHIFT:
                result = l >> r;
                break;
            case EQ:
                result = l == r ? 1 : 0;
                break;
            case NEQ:
                result = l != r ? 1 : 0;
                break;
            case LT:
                result = l < r ? 1 : 0;
                break;
            case GT:
                result = l > r ? 1 : 0;
                break;
            case LEQ:
                result = l <= r ? 1 : 0;
                break;
            case GEQ:
                result = l >= r ? 1 : 0;
                break;
            default:
                throw new InternalCompilerError("Invalid binary operation");
    		}
    		
    		return new IRConst(result);
    		
    	}
    		
    	
    }

	@Override
	public OpTarget genAssem(StringWriter sw, IRFuncDecl f, FuncSymbolTable funcs) {
		// TODO Auto-generated method stub
		f.count++;
		operand = new OpTarget(f.count);
		
		String opStr = "";
		switch(this.opType()) {
		case ADD:
			opStr = "addq";
			break;
		case SUB:
			opStr = "subq";
			break;
		case MUL:
		case HMUL:
			opStr = "imulq";
			break;
		case DIV:
		case MOD:
			opStr = "idivq";
			break;
		case AND:
			opStr = "andq";
			break;
		case OR:
			opStr = "orq";
			break;
		case XOR:
			opStr = "xorq";
			break;
		case EQ:
		case NEQ:
		case LT:
		case GT:
		case LEQ:
		case GEQ:
			opStr = "cmpq";
			break;
		default:
		}
		
		switch(this.opType()) {
		case ADD:
		case SUB:

            if (
                left instanceof IRBinOp && 
                (((IRBinOp) left).opType() == IRBinOp.OpType.MUL) &&
                this.opType() == OpType.ADD
            ) {
                IRBinOp _left = (IRBinOp) left;
                OpTarget _r = right.genAssem(sw, f, funcs);

                if (_left.left() instanceof IRConst) {
                    sw.write("  movq    " + _r.getTarget(false) + ", %r10\n");
                    IRConst _const = (IRConst) (_left.left());
                    OpTarget r = _left.right().genAssem(sw, f, funcs);
                    sw.write("  movq    " + r.getTarget(false) + ", %r11\n");
                    sw.write("  lea     (%r10, %r11, " + _const.value() + "), %r11\n");
                    sw.write("  movq    %r11, " + operand.getTarget(false) + "\n");
                } else if (_left.right() instanceof IRConst) {
                    sw.write("  movq    " + _r.getTarget(false) + ", %r10\n");
                    IRConst _const = (IRConst) (_left.right());
                    OpTarget l = _left.left().genAssem(sw, f, funcs);
                    sw.write("  movq    " + l.getTarget(false) + ", %r11\n");
                    sw.write("  lea     (%r10, %r11, " + _const.value() + "), %r11\n");
                    sw.write("  movq    %r11, " + operand.getTarget(false) + "\n");
                } else {
                    OpTarget l = left.genAssem(sw, f, funcs);
                    OpTarget r = right.genAssem(sw, f, funcs);
                    if(l.type == OpTarget.TempType.TEMP && r.type == OpTarget.TempType.TEMP)
                        sw.write("# BINOP t" + l.num + " and t" + r.num + "\n");
                    sw.write("  movq    " + l.getTarget(false) + ", %rax\n"
                            +"  " + opStr + "   " + r.getTarget(false) + ", %rax\n"
                            +"  movq    %rax, " + operand.getTarget(false) + "\n");
                }
            }
            else if (
                right instanceof IRBinOp &&
                (((IRBinOp) right).opType() == IRBinOp.OpType.MUL) 
            ) {
                OpTarget _l = left.genAssem(sw, f, funcs);
                IRBinOp _right = (IRBinOp) right;
                
                if (_right.left() instanceof IRConst) {
                    sw.write("  movq    " + _l.getTarget(false) + ", %r11\n");
                    IRConst _const = (IRConst) (_right.left());
                    OpTarget r = _right.right().genAssem(sw, f, funcs);
                    sw.write("  movq    " + r.getTarget(false) + ", %r10\n");
                    sw.write("  lea     (%r11, %r10, " + (this.opType() == OpType.ADD ? _const.value() : -_const.value()) + "), %r11\n");
                    sw.write("  movq    %r11, " + operand.getTarget(false) + "\n");
                }
                else if (_right.right() instanceof IRConst) {
                    sw.write("  movq    " + _l.getTarget(false) + ", %r11\n");
                    IRConst _const = (IRConst) (_right.right());
                    OpTarget l = _right.left().genAssem(sw, f, funcs);
                    sw.write("  movq    " + l.getTarget(false) + ", %r10\n");
                    sw.write("  lea     (%r11, %r10, " + (this.opType() == OpType.ADD ? _const.value() : -_const.value()) + "), %r11\n");
                    sw.write("  movq    %r11, " + operand.getTarget(false) + "\n");
                } 
                else {
                    OpTarget l = left.genAssem(sw, f, funcs);
                    OpTarget r = right.genAssem(sw, f, funcs);
                    if(l.type == OpTarget.TempType.TEMP && r.type == OpTarget.TempType.TEMP)
                        sw.write("# BINOP t" + l.num + " and t" + r.num + "\n");
                    sw.write("  movq    " + l.getTarget(false) + ", %rax\n"
                            +"  " + opStr + "   " + r.getTarget(false) + ", %rax\n"
                            +"  movq    %rax, " + operand.getTarget(false) + "\n");
                }
            }
			else {                
				OpTarget l = left.genAssem(sw, f, funcs);
				OpTarget r = right.genAssem(sw, f, funcs);
				if(l.type == OpTarget.TempType.TEMP && r.type == OpTarget.TempType.TEMP)
					sw.write("# BINOP t" + l.num + " and t" + r.num + "\n");
				sw.write("	movq	" + l.getTarget(false) + ", %rax\n"
						+" 	" + opStr + "	" + r.getTarget(false) + ", %rax\n"
						+"	movq	%rax, " + operand.getTarget(false) + "\n");
			}
			break;
		case MUL:
		case HMUL:
			if(false) {
				//match tiles
			}
			else {
				OpTarget l = left.genAssem(sw, f, funcs);
				OpTarget r = right.genAssem(sw, f, funcs);
				sw.write("	movq	" + l.getTarget(false) + ", %rax\n"
						+" 	" + opStr + "	" + r.getTarget(false) + "\n");
				if(this.opType() == OpType.MUL)
					sw.write("	movq	%rax, " + operand.getTarget(false) + "\n");
				else
					sw.write("	movq	%rdx, " + operand.getTarget(false) + "\n");
			}
			break;
		case DIV:
		case MOD:
			if(false) {
				// NO-OP
			}
			else {
				OpTarget l = left.genAssem(sw, f, funcs);
				OpTarget r = right.genAssem(sw, f, funcs);
				sw.write("	xorq	%rdx, %rdx\n"
						+"	movq	" + l.getTarget(false) + ", %rax\n"
						+" 	" + opStr + "	" + r.getTarget(false) + "\n");
				
				if(this.opType() == OpType.DIV)
					sw.write("	movq	%rax, " + operand.getTarget(false) + "\n");
				else
					sw.write("	movq	%rdx, " + operand.getTarget(false) + "\n");
			}
			break;
		case AND:
		case OR:
		case XOR:
			if(false) {
				// NO-OP
			}
			else {
				OpTarget l = left.genAssem(sw, f, funcs);
				OpTarget r = right.genAssem(sw, f, funcs);
				sw.write("	movq	" + l.getTarget(false) + ", %rax\n"
						+" 	" + opStr + "	" + r.getTarget(false) + ", %rax\n"
						+"	movq	%rax, " + operand.getTarget(false) + "\n");

			}
			break;
		case EQ:
		case NEQ:
		case LT:
		case GT:
		case LEQ:
		case GEQ:
			if(false) {
				// NO-OP
			}
			else {
				OpTarget l = left.genAssem(sw, f, funcs);
				OpTarget r = right.genAssem(sw, f, funcs);
				sw.write("	movq	" + l.getTarget(false) + ", %rax\n"
						+"	" + opStr + "	" + r.getTarget(false) + ", %rax\n");
				
				switch(this.opType()) {
				case EQ:
					sw.write("	je	L_BINOP_CMP_T_" + (++cmpLabelCount) + "\n");
					break;
				case NEQ:
					sw.write("	jne	L_BINOP_CMP_T_" + (++cmpLabelCount) + "\n");			
					break;
				case LT:
					sw.write("	jl	L_BINOP_CMP_T_" + (++cmpLabelCount) + "\n");
					break;
				case GT:
					sw.write("	jg	L_BINOP_CMP_T_" + (++cmpLabelCount) + "\n");
					break;
				case LEQ:
					sw.write("	jle	L_BINOP_CMP_T_" + (++cmpLabelCount) + "\n");
					break;
				case GEQ:
					sw.write("	jge	L_BINOP_CMP_T_" + (++cmpLabelCount) + "\n");
					break;
				}
				
				sw.write("	movq	$0, " + operand.getTarget(false) + "\n"
						+"	jmp	L_BINOP_CMP_END_" + cmpLabelCount + "\n"
						+"L_BINOP_CMP_T_" + cmpLabelCount + ":\n"
						+"	movq	$1, " + operand.getTarget(false) + "\n"
						+"L_BINOP_CMP_END_" + cmpLabelCount + ":\n");
			}
			break;
		default:
				
		}
		
		return operand;
	}
   
    
}
