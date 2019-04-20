package bt.compiler;

import java.nio.ByteBuffer;

import org.objectweb.asm.tree.MethodNode;

public class Method {
	ByteBuffer code;
	MethodNode node;
	
	boolean parsing = false;
	int nLocalVars = 0;
	int address;
}
