package edu.cornell.cs.cs4120.xic.ir;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import edu.cornell.cs.cs4120.xic.ir.visit.AggregateVisitor;
import edu.cornell.cs.cs4120.xic.ir.visit.CheckCanonicalIRVisitor;
import edu.cornell.cs.cs4120.xic.ir.visit.IRVisitor;
import zr54.assembly.OpTarget;
import zr54.typechecker.FuncSymbolTable;

/**
 * An intermediate representation for a sequence of statements
 * SEQ(s1,...,sn)
 */
public class IRSeq extends IRStmt {
    private List<IRStmt> stmts;

    /**
     * @param stmts the statements
     */
    public IRSeq(IRStmt... stmts) {
    	super();
    	this.stmts = Arrays.asList(stmts);
    	for (int i = 0; i < this.stmts.size(); i++)
    		this.children.add(this.stmts.get(i));
    }

    /**
     * Create a SEQ from a list of statements. The list should not be modified subsequently.
     * @param stmts the sequence of statements
     */
    public IRSeq(List<IRStmt> stmts) {
    	super();
        this.stmts = stmts;
        for (int i = 0; i < this.stmts.size(); i++)
        	this.children.add(this.stmts.get(i));
    		
    }
    
    public void updateChildren() {
    	for (int i = 0; i < this.children.size(); i++)
    		this.stmts.set(i, (IRStmt) this.children.get(i));
    }

    public List<IRStmt> stmts() {
        return stmts;
    }

    @Override
    public String label() {
        return "SEQ";
    }

    @Override
    public IRNode visitChildren(IRVisitor v) {
        boolean modified = false;

        List<IRStmt> results = new ArrayList<>(stmts.size());
        for (IRStmt stmt : stmts) {
            IRStmt newStmt = (IRStmt) v.visit(this, stmt);
            if (newStmt != stmt) modified = true;
            results.add(newStmt);
        }

        if (modified) return new IRSeq(results);

        return this;
    }

    @Override
    public <T> T aggregateChildren(AggregateVisitor<T> v) {
        T result = v.unit();
        for (IRStmt stmt : stmts)
            result = v.bind(result, v.visit(stmt));
        return result;
    }

    @Override
    public CheckCanonicalIRVisitor checkCanonicalEnter(
            CheckCanonicalIRVisitor v) {
        return v.enterSeq();
    }

    @Override
    public boolean isCanonical(CheckCanonicalIRVisitor v) {
        return !v.inSeq();
    }

    @Override
    public void printSExp(SExpPrinter p) {
        p.startUnifiedList();
        p.printAtom("SEQ");
        for (IRStmt stmt : stmts)
            stmt.printSExp(p);
        p.endList();
    }
    
    /**
     * Do constant folding. If any children can be folded, replace it with a IRConst node.
     * @return if this node can be folded into a constant, return the IRConst node
     * 		   otherwise return null
     */
    @Override 
    public IRConst doConstFolding() {
    	for(IRNode n : stmts) {
    		n.doConstFolding();
    	}
    	return null;
    }

	@Override
	public OpTarget genAssem(StringWriter sw, IRFuncDecl f, FuncSymbolTable funcs) {
		// TODO Auto-generated method stub
		for(IRStmt s : stmts) 
			s.genAssem(sw, f, funcs);
		return operand;
	}
}
