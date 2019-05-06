package bt.compiler;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

public class Method {

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

	ArrayList<Jump> jumps = new ArrayList<>();
	ByteBuffer code;
	MethodNode node;
	
	int address;
}
