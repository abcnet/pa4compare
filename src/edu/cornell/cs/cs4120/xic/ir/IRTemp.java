package edu.cornell.cs.cs4120.xic.ir;

import java.io.StringWriter;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import edu.cornell.cs.cs4120.xic.ir.interpret.Configuration;
import zr54.assembly.OpTarget;
import zr54.typechecker.FuncSymbolTable;

/**
 * An intermediate representation for a temporary register
 * TEMP(name)
 */
public class IRTemp extends IRExpr {
    private String name;
    
    /**
     *
     * @param name name of this temporary register
     */
    public IRTemp(String name) {
    	super();
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public String label() {
        return "TEMP(" + name + ")";
    }

    @Override
    public void printSExp(SExpPrinter p) {
        p.startList();
        p.printAtom("TEMP");
        p.printAtom(name);
        p.endList();
    }
    
    /**
     * Do constant folding. If any children can be folded, replace it with a IRConst node.
     * @return if this node can be folded into a constant, return the IRConst node
     * 		   otherwise return null
     */
    @Override 
    public IRConst doConstFolding() {
    	return null;
    }

	@Override
	public OpTarget genAssem(StringWriter sw, IRFuncDecl f, FuncSymbolTable funcs) {
		// TODO Auto-generated method stub
		String rawFuncName = f.name().substring(2, f.name().lastIndexOf('_'));
		boolean gt2 = funcs.lookup(rawFuncName).getFunctionReturnTypes().getTuple().size() > 2;
		if(name.startsWith(Configuration.ABSTRACT_ARG_PREFIX)) {
			int idx = Integer.parseInt(name.substring(Configuration.ABSTRACT_ARG_PREFIX.length()));
			operand = new OpTarget(idx, gt2);
		}
		else if(name.startsWith(Configuration.ABSTRACT_RET_PREFIX)) {
			int idx = Integer.parseInt(name.substring(Configuration.ABSTRACT_RET_PREFIX.length()));
			operand = new OpTarget(OpTarget.TempType.RET, idx);
		}
		else {
			if(f.tempNodeTable.containsKey(this.name)) {
				int tempIndex = f.tempNodeTable.get(this.name);
				operand = new OpTarget(tempIndex);
			} 
			else {
				f.count++;
				sw.write("# TEMP " + this.name + " is t" + f.count + " on stack.\n");
				operand = new OpTarget(f.count);
				f.tempNodeTable.put(this.name, f.count);
			}
		}
		return operand;
	}
}
