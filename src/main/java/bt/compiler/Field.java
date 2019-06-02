package bt.compiler;

import org.objectweb.asm.tree.FieldNode;

/**
 * Class representing a smart contract field.
 * 
 * @author jjos
 */
public class Field {
	FieldNode node;
	int size;
	int address;

	/**
	 * @return the name of this field
	 */
	public String getName(){
		return node.name;
	}

	/**
	 * @return the address of this field
	 */
	public int getAddress(){
		return address;
	}
}
