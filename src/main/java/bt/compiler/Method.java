package bt.compiler;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Class holding information about a method belonging to a Contract.
 * 
 * @author jjos
 */
public class Method {

	public static final int MAX_ARGS = 3;

	static class Jump {
		int position;
		LabelNode label;
		Method method;

		Jump(int p, LabelNode l) {
			position = p;
			label = l;
		}
		Jump(int p, Method m) {
			position = p;
			method = m;
		}
	}

	/**
	 * @return the hash of this method (for external calling)
	 */
	public long getHash(){
		return hash;
	}

	/**
	 * @return the number of arguments required by this method
	 */
	public int getNArgs(){
		return nargs;
	}

	/**
	 * @return the name of this method
	 */
	public String getName(){
		return node.name;
	}

	ArrayList<Jump> jumps = new ArrayList<>();
	ByteBuffer code;
	MethodNode node;
	int nargs;
	int localArgPos[] = new int[MAX_ARGS];
	int localArgSize[] = new int[MAX_ARGS];
	int localArgTotal;

	long hash;
	
	int address;
}
