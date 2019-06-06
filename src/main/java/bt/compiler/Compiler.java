package bt.compiler;

import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bt.*;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import burst.kit.burst.BurstCrypto;
import burst.kit.entity.BurstID;

/**
 * Class to convert a {@link Contract} java bytecode to ciyam bytecode.
 * 
 * @author jjos
 */
public class Compiler {

	public static final CompilerVersion currentVersion = WarningProcessor.currentVersion;

	public static final String INIT_METHOD = "<init>";
	public static final String MAIN_METHOD = "main";
	public static final String TX_RECEIVED_METHOD = "txReceived";
	public static final int MAX_SIZE = 10 * 256;

	private static final String UNEXPECTED_ERROR = "Unexpected error, please report at https://github.com/burst-apps-team/blocktalk/issues";

	ClassNode cn;
	ByteBuffer code;

	LinkedList<StackVar> stack = new LinkedList<>();
	HashMap<String, Method> methods = new HashMap<>();
	HashMap<String, Field> fields = new HashMap<>();
	HashMap<LabelNode, Integer> labels = new HashMap<>();

	String className;

	int lastFreeVar;
	boolean useLastTx;
	int lastTxReceived;
	int lastTxTimestamp;
	int tmpVar1, tmpVar2, tmpVar3, tmpVar4;
	int localStart;

	/** If we have public methods other than txReceived */
	boolean hasPublicMethods;

	public class Error {
		AbstractInsnNode node;
		String message;

		Error(AbstractInsnNode node, String message) {
			this.node = node;
			this.message = message;
		}

		public String getMessage() {
			return message;
		}
	}

	ArrayList<Error> errors = new ArrayList<>();

	public Compiler(Class<? extends Contract> clazz) throws IOException {
		this.className = clazz.getName();
		TargetCompilerVersion targetCompilerVersion = clazz.getAnnotation(TargetCompilerVersion.class);
		if (targetCompilerVersion == null) {
			System.err.println("WARNING: Target compiler version not specified");
		} else if (targetCompilerVersion.value() != currentVersion) {
			if (targetCompilerVersion.value().ordinal() > currentVersion.ordinal())
				System.err.println(
						"WARNING: Target compiler version newer than compiler version. Newer features may not compile or work.");
			if (targetCompilerVersion.value().ordinal() < currentVersion.ordinal())
				System.err.println(
						"WARNING: Target compiler version older than compiler version. Contract source code may be incompatible.");
		}

		// read in, build classNode
		ClassNode classNode = new ClassNode();
		ClassReader cr = new ClassReader(className);
		cr.accept(classNode, 0);

		this.cn = classNode;
	}

	static final int STACK_THIS = 0;
	static final int STACK_FIELD = 1;
	static final int STACK_VAR_ADDRESS = 2;
	static final int STACK_CONSTANT = 3;
	static final int STACK_PUSH = 4;

	class StackVar {
		public StackVar(int type, Object value) {
			this.type = type;
			if (value instanceof String)
				this.svalue = (String) value;
			else if (value instanceof Long)
				this.lvalue = (Long) value;
			else if (value instanceof Integer)
				this.address = (Integer) value;

		}

		int type;
		Integer address;
		Long lvalue;
		String svalue;

		@Override
		public String toString() {
			switch (type) {
			case STACK_THIS:
				return "this";
			case STACK_FIELD:
				return "field: " + address;
			case STACK_VAR_ADDRESS:
				return "var addr: " + address;
			case STACK_PUSH:
				return "ustack";
			case STACK_CONSTANT:
			default:
				return "cst: " + (svalue != null ? svalue : lvalue != null ? lvalue : address);
			}
		}
	}

	public ByteBuffer getCode() {
		return code;
	}

	private void readFields() {

		if (!cn.superName.replace('/', '.').equals(Contract.class.getName())) {
			addError(null, "A contract should derive from " + Contract.class.getName());
		}

		errors.clear();
		lastFreeVar = 0;

		for (FieldNode f : cn.fields) {
			// System.out.println("field name:" + f.name);
			int nvars = 0;

			if (Modifier.isFinal(f.access) && Modifier.isStatic(f.access))
				continue; // we simply skip this

			String desc = f.desc;
			if (desc.startsWith("L"))
				desc = desc.substring(1, desc.length() - 1).replace('/', '.');

			if (desc.equals("J"))
				nvars = 1; // long
			else if (desc.equals("Z"))
				nvars = 1; // boolean
			else if (desc.equals("I"))
				nvars = 1; // integer
			else if (desc.equals(Address.class.getName()))
				nvars = 1;
			else if (desc.equals(Transaction.class.getName()))
				nvars = 1;
			else if (desc.equals(Timestamp.class.getName()))
				nvars = 1;
			else if (desc.equals(Register.class.getName()))
				nvars = 4;

			/*
			 * some related operators are still missing else if (f.desc.equals("B")) nvars =
			 * 1; // byte else if (f.desc.equals("S")) nvars = 1; // short
			 */

			if (nvars == 0) {
				addError(null, f.name + ", invalid field type: " + f.desc);
				continue;
			}

			Field fld = new Field();
			fld.address = lastFreeVar;
			fld.node = f;
			fld.size = nvars;
			fields.put(f.name, fld);

			lastFreeVar += nvars;
		}

		// Variables are reserved the lastTxReceived so we
		// can easily have the 'current' tx variable available
		lastTxTimestamp = lastFreeVar++;
		lastTxReceived = lastFreeVar++;

		// Temporary variables come last (used for pushing and poping values from user
		// stack)
		tmpVar1 = lastFreeVar++;
		tmpVar2 = lastFreeVar++;
		tmpVar3 = lastFreeVar++;
		tmpVar4 = lastFreeVar++;

		localStart = lastFreeVar++;
	}

	public void compile() {
		readFields();
		readMethods();
	}

	private void initialCode() {
		// First add the jump for the constructor
		Method initMethod = methods.get(INIT_METHOD);
		if (initMethod.code.position() > 1) {
			// only if we actually have a construction (it is not just the RET command)
			code.put(OpCode.e_op_code_JMP_SUB);
			code.putInt(methods.get(INIT_METHOD).address);
		}

		// set the local variables start position
		code.put(OpCode.e_op_code_SET_VAL);
		code.putInt(localStart);
		code.putLong(lastFreeVar);

		// The starting point for future calls (PCS)
		code.put(OpCode.e_op_code_SET_PCS);

		if (useLastTx) {
			// put the last transaction received in A (after the last timestamp)
			code.put(OpCode.e_op_code_EXT_FUN_DAT);
			code.putShort(OpCode.A_To_Tx_After_Timestamp);
			code.putInt(lastTxTimestamp);

			// get the value from A1
			code.put(OpCode.e_op_code_EXT_FUN_RET);
			code.putShort(OpCode.Get_A1);
			code.putInt(lastTxReceived);

			// if zero we will FINISH, otherwise continue
			// TODO: check if this is really necessary, since there should always be a
			// transaction
			// code.put(OpCode.e_op_code_BNZ_DAT);
			// code.putInt(lastTxReceived);
			// code.put((byte) 7);
			// code.put(OpCode.e_op_code_FIN_IMD);

			// Store the timestamp of the last transaction
			code.put(OpCode.e_op_code_EXT_FUN_RET);
			code.putShort(OpCode.Get_Timestamp_For_Tx_In_A);
			code.putInt(lastTxTimestamp);
		}

		if (hasPublicMethods) {
			// external method calls here
			code.put(OpCode.e_op_code_EXT_FUN);
			code.putShort(OpCode.Message_From_Tx_In_A_To_B);

			// First bytes will have the function to call (using tmpVar4)
			code.put(OpCode.e_op_code_EXT_FUN_RET);
			code.putShort(OpCode.Get_B1);
			code.putInt(tmpVar4);

			for (Method m : methods.values()) {
				if (m.node.name.equals(MAIN_METHOD) || m.node.name.equals(TX_RECEIVED_METHOD)
						|| !Modifier.isPublic(m.node.access))
					continue;

				if (m.node.name.equals(INIT_METHOD) && m.code.position() < 2)
					continue; // empty constructor

				code.put(OpCode.e_op_code_SET_VAL);
				code.putInt(tmpVar1);
				code.putLong(m.hash);

				code.put(OpCode.e_op_code_SUB_DAT);
				code.putInt(tmpVar1);
				code.putInt(tmpVar4);

				code.put(OpCode.e_op_code_BNZ_DAT);
				code.putInt(tmpVar1);
				code.put((byte) (12 + m.nargs * 7));

				// load the arguments on the local vars
				for (int i = 0; i < m.nargs; i++) {
					code.put(OpCode.e_op_code_EXT_FUN_RET);
					code.putShort((short) (OpCode.Get_B1 + i + 1));
					code.putInt(localStart + 1 + m.localArgPos[i]);
				}
				// call the method
				code.put(OpCode.e_op_code_JMP_SUB);
				code.putInt(m.address);
				// end this run
				code.put(OpCode.e_op_code_FIN_IMD);
			}
		}

		// call the txReceived method
		code.put(OpCode.e_op_code_JMP_SUB);
		code.putInt(methods.get(TX_RECEIVED_METHOD).address);

		// end this run
		code.put(OpCode.e_op_code_FIN_IMD);
	}

	public void link() {
		code = ByteBuffer.allocate(MAX_SIZE);
		code.order(ByteOrder.LITTLE_ENDIAN);

		initialCode();
		int startMethodsPosition = code.position();

		// determine the address of each method
		int address = startMethodsPosition; // position of the first method
		for (Method m : methods.values()) {
			if (m.node.name.equals(INIT_METHOD) && m.code.position() < 2)
				continue; // empty constructor
			m.address = address;
			address += m.code.position();
		}

		ByteBuffer posBuffer = ByteBuffer.allocate(4);
		posBuffer.order(ByteOrder.LITTLE_ENDIAN);

		// resolve all jumps
		for (Method m : methods.values()) {
			// resolve all offsets
			for (Method.Jump j : m.jumps) {
				int jaddress = 0;
				if (j.method != null)
					jaddress = j.method.address;
				else {
					Integer position = labels.get(j.label);
					if (position == null) {
						addError(j.label, "Label not found: " + j.label.getLabel());
						continue;
					}
					jaddress = position + m.address;
				}
				byte[] code = m.code.array();
				posBuffer.putInt(jaddress);
				posBuffer.clear();
				byte[] posBytes = posBuffer.array();
				for (int i = 0; i < posBytes.length; i++) {
					code[j.position + i] = posBytes[i];
				}
			}
		}

		// now with the correct positions
		code.rewind();
		initialCode();

		// add methods
		for (Method m : methods.values()) {
			if (m.node.name.equals(INIT_METHOD) && m.code.position() < 2)
				continue; // empty constructor
			code.put(m.code.array(), 0, m.code.position());
		}
	}

	private void readMethods() {
		useLastTx = false;
		hasPublicMethods = false;

		// First list all methods available
		for (MethodNode mnode : cn.methods) {
			if (mnode.name.equals(MAIN_METHOD))
				continue; // skyp the main function (should be for deubgging only)

			Method m = new Method();
			m.node = mnode;
			countMethodParameters(m);
			m.hash = getMethodSignature(m);

			methods.put(mnode.name, m);

			if (!mnode.name.equals(TX_RECEIVED_METHOD) && Modifier.isPublic(mnode.access)) {
				hasPublicMethods = true;
				useLastTx = true;
			}
		}

		if (errors.size() > 0)
			return;

		// Then parse
		for (Method m : methods.values()) {
			System.out.print("** METHOD: " + m.node.name);
			if (m.hash != 0)
				System.out.print(" hash: " + m.hash);
			System.out.println();
			parseMethod(m);
		}
	}

	/**
	 * @return the methods
	 */
	public Collection<Method> getMethods() {
		return methods.values();
	}

	/**
	 * @param name
	 * @return the method for the given name
	 */
	public Method getMethod(String name) {
		return methods.get(name);
	}

	/**
	 * @return the fields
	 */
	public Collection<Field> getFields() {
		return fields.values();
	}

	/**
	 * 
	 * @param name
	 * @return the field for the given name
	 */
	public Field getField(String name) {
		return fields.get(name);
	}

	/**
	 * Push the variable on the given address to the stack.
	 */
	StackVar pushVar(Method m, int address) {
		StackVar v = new StackVar(address >= tmpVar1 ? STACK_PUSH : STACK_FIELD, address);
		stack.add(v);

		if (v.type == STACK_PUSH) {
			// is a tmp var, not a field, push to AT stack
			m.code.put(OpCode.e_op_code_PSH_DAT);
			m.code.putInt(address);
		}
		return v;
	}

	/**
	 * Pop the lastest added variable from the stack and store on the given address.
	 * 
	 * @param m
	 * @param destAddress
	 */
	StackVar popVar(Method m, int destAddress, boolean forceCopy) {
		StackVar var = stack.pollLast();

		if (var.type == STACK_PUSH) {
			// is a tmp var, pop needed
			m.code.put(OpCode.e_op_code_POP_DAT);
			m.code.putInt(destAddress);
			var.address = destAddress;
		} else if (var.type == STACK_FIELD) {
			if (forceCopy) {
				m.code.put(OpCode.e_op_code_SET_DAT);
				m.code.putInt(destAddress);
				m.code.putInt(var.address);
				var.address = destAddress;
			}
			// otherwise, do nothing
		} else if (var.type == STACK_THIS) {
			// do nothing
		} else
			addError(null, UNEXPECTED_ERROR);
		return var;
	}

	private void parseMethod(Method m) {
		ByteBuffer code = ByteBuffer.allocate(Compiler.MAX_SIZE);
		code.order(ByteOrder.LITTLE_ENDIAN);

		m.code = code;

		if (m.node.name.equals(INIT_METHOD)) {
			if (!Modifier.isPublic(m.node.access))
				addError(m.node.instructions.get(0), "Contract constructor must be public");

			if (!m.node.desc.equals("()V"))
				addError(m.node.instructions.get(0), "Contract constructor cannot have arguments");
		}

		if (Modifier.isPublic(m.node.access) && !m.node.name.equals(TX_RECEIVED_METHOD)
				&& !m.node.name.equals(INIT_METHOD) && !m.node.name.equals(MAIN_METHOD)) {
			if (m.nargs > Method.MAX_ARGS) {
				addError(m.node.instructions.getFirst(),
						"Public functions with more than " + Method.MAX_ARGS + " arguments are not supported");
			}
		}

		StackVar arg1, arg2;

		Iterator<AbstractInsnNode> ite = m.node.instructions.iterator();
		while (ite.hasNext()) {
			AbstractInsnNode insn = ite.next();

			int opcode = insn.getOpcode();

			if (opcode == -1) {
				// This is a label or line number information
				if (insn instanceof LabelNode) {
					LabelNode ln = (LabelNode) insn;
					labels.put(ln, code.position());
					System.out.println("label: " + ln.getLabel());
				}
				/*
				 * else if(insn instanceof LineNumberNode) { LineNumberNode ln =
				 * (LineNumberNode) insn;
				 * 
				 * System.out.println("line: " + ln.line); } else if(insn instanceof FrameNode)
				 * { FrameNode fn = (FrameNode) insn;
				 * 
				 * System.out.println("frame type: " + fn.getType()); } else {
				 * System.err.println(opcode); }
				 */
				continue;
			}

			if (stack.size() > 0) {
				System.out.print("Stack");
				for (StackVar var : stack) {
					System.out.print(": " + var.toString());
				}
				System.out.println();
			}

			switch (opcode) {
			case ILOAD:
			case LLOAD:
			case ALOAD:
				if (insn instanceof VarInsnNode) {
					VarInsnNode vi = (VarInsnNode) insn;
					if (vi.var > 0) {
						// tmpVar2 have the local index, starting at localStart
						code.put(OpCode.e_op_code_SET_DAT);
						code.putInt(tmpVar2);
						code.putInt(localStart);

						// increment the index if 2 or higher
						for (int i = 0; i < vi.var - 1; i++) {
							code.put(OpCode.e_op_code_INC_DAT);
							code.putInt(tmpVar2);
						}
						// set tmpVar1 using the index on tmpVar2
						code.put(OpCode.e_op_code_SET_IND);
						code.putInt(tmpVar1);
						code.putInt(tmpVar2);

						pushVar(m, tmpVar1);
					} else {
						// local 0 is 'this'
						stack.add(new StackVar(STACK_THIS, null));
					}
					System.out.println((opcode < ISTORE ? "load" : "store") + " local: " + vi.var);
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}
				break;

			case ISTORE:
			case LSTORE:
			case ASTORE:
				if (insn instanceof VarInsnNode) {
					VarInsnNode vi = (VarInsnNode) insn;
					if (vi.var == 0)
						addError(insn, UNEXPECTED_ERROR);
					// local 0 is 'this', others are stored after 'localStart' variable

					arg1 = popVar(m, tmpVar1, false);
					System.out.println("store local: " + vi.var);

					// tmpVar2 have the local index, starting at localStart
					code.put(OpCode.e_op_code_SET_DAT);
					code.putInt(tmpVar2);
					code.putInt(localStart);

					// increment the index if 2 or higher
					for (int i = 0; i < vi.var - 1; i++) {
						code.put(OpCode.e_op_code_INC_DAT);
						code.putInt(tmpVar2);
					}
					// set var using the index on tmpVar2
					code.put(OpCode.e_op_code_IND_DAT);
					code.putInt(tmpVar2);
					code.putInt(arg1.address);
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}
				break;

			case I2L:
				break; // nothing needed
			case L2I: // long 2 int
			case I2B: // int 2 byte
			case I2C: // int 2 char
			case I2S: // int 2 short
				arg1 = popVar(m, tmpVar1, true);
				code.put(OpCode.e_op_code_SET_VAL);
				code.putInt(tmpVar2);
				switch (opcode) {
				case L2I:
					code.putLong(0xFFFFFFFFL);
					break;
				case I2B:
					code.putLong(0xFFL);
					break;
				case I2C:
				case I2S:
					code.putLong(0xFFFFL);
					break;
				}
				code.put(OpCode.e_op_code_AND_DAT);
				code.putInt(arg1.address);
				code.putInt(tmpVar2);

				pushVar(m, arg1.address);
				break;

			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
				code.put(OpCode.e_op_code_SET_VAL);
				code.putInt(tmpVar1);
				code.putLong(opcode - ICONST_0);

				pushVar(m, tmpVar1);
				System.out.println("iconstant : " + (opcode - ICONST_0));
				break;

			case LCONST_0:
			case LCONST_1:
				code.put(OpCode.e_op_code_SET_VAL);
				code.putInt(tmpVar1);
				code.putLong(opcode - LCONST_0);

				pushVar(m, tmpVar1);
				System.out.println("lconstant : " + (opcode - LCONST_0));
				break;

			case ACONST_NULL:
				code.put(OpCode.e_op_code_CLR_DAT);
				code.putInt(tmpVar1);

				pushVar(m, tmpVar1);
				System.out.println("load null");
				break;

			case IADD:
			case LADD:
			case ISUB:
			case LSUB:
			case IDIV:
			case LDIV:
			case IMUL:
			case LMUL:
			case IREM:
			case LREM:
			case IAND:
			case LAND:
				// we should have two arguments on the stack
				arg2 = popVar(m, tmpVar2, false);
				arg1 = popVar(m, tmpVar1, true);

				switch (opcode) {
				case ISUB:
				case LSUB:
					System.out.println("sub");
					code.put(OpCode.e_op_code_SUB_DAT);
					break;
				case IMUL:
				case LMUL:
					System.out.println("mul");
					code.put(OpCode.e_op_code_MUL_DAT);
					break;
				case IDIV:
				case LDIV:
					System.out.println("div");
					code.put(OpCode.e_op_code_DIV_DAT);
					break;
				case IREM:
				case LREM:
					System.out.println("mod");
					code.put(OpCode.e_op_code_MOD_DAT);
					break;
				case IAND:
				case LAND:
					System.out.println("AND");
					code.put(OpCode.e_op_code_AND_DAT);
					break;
				default:
					System.out.println("add");
					code.put(OpCode.e_op_code_ADD_DAT);
					break;
				}
				code.putInt(arg1.address);
				code.putInt(arg2.address);

				pushVar(m, tmpVar1);
				break;
			case INEG:
			case LNEG:
				System.out.println("neg");

				arg1 = popVar(m, tmpVar2, false);

				code.put(OpCode.e_op_code_CLR_DAT);
				code.putInt(tmpVar1);
				code.put(OpCode.e_op_code_SUB_DAT);
				code.putInt(tmpVar1);
				code.putInt(arg1.address);

				pushVar(m, tmpVar1);
				break;

			case IRETURN:
			case LRETURN:
			case ARETURN:
				// remove the return value from the stack
				stack.pollLast();
			case RETURN:
				// Recalling that every method call will use JMP_SUB
				System.out.println("return");
				code.put(OpCode.e_op_code_RET_SUB);
				break;

			case DUP: // duplicate the value on top of the stack
			{
				StackVar var = popVar(m, tmpVar1, false);
				if (var.type == STACK_THIS) {
					stack.addLast(var);
					stack.addLast(var);
				} else if (var.type == STACK_PUSH) {
					pushVar(m, var.address);
					pushVar(m, var.address);
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}

				System.out.println("dup");
			}
				break;

			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
			case INVOKEDYNAMIC:
				if (insn instanceof MethodInsnNode) {
					MethodInsnNode mi = (MethodInsnNode) insn;
					String owner = mi.owner.replace('/', '.');

					System.out.println("invoke, name:" + mi.name + " owner:" + owner);

					if (owner.equals(Contract.class.getName())) {
						if (mi.name.equals(INIT_METHOD)) {
							// Contract super constructor call, do nothing
							stack.pollLast(); // remove the "this" from stack
						} else {
							addError(insn, "Cannot access " + owner + "." + mi.name);
						}
					} else if (owner.equals(Timestamp.class.getName())) {
						// a call on the timestamp object
						if (mi.name.equals("ge") || mi.name.equals("le")) {
							// we should have two arguments
							arg1 = popVar(m, tmpVar1, false);
							arg2 = popVar(m, tmpVar2, false);

							code.put(OpCode.e_op_code_CLR_DAT);
							code.putInt(tmpVar3);
							if (mi.name.equals("ge"))
								code.put(OpCode.e_op_code_BLT_DAT);
							else
								code.put(OpCode.e_op_code_BGT_DAT);
							code.putInt(arg2.address);
							code.putInt(arg1.address);
							code.put((byte) 0x0f); // offset
							code.put(OpCode.e_op_code_INC_DAT);
							code.putInt(tmpVar3);
							pushVar(m, tmpVar3);
						} else if (mi.name.equals("addMinutes")) {
							// we should have two arguments
							arg2 = popVar(m, tmpVar2, false); // minutes
							arg1 = popVar(m, tmpVar1, false); // the timestamp

							code.put(OpCode.e_op_code_EXT_FUN_RET_DAT_2);
							code.putShort(OpCode.Add_Minutes_To_Timestamp);
							code.putInt(tmpVar3);
							code.putInt(arg1.address);
							code.putInt(arg2.address);
							pushVar(m, tmpVar3);
						} else if (mi.name.equals("getValue")) {
							// it is the timestamp object itself (already on stack)
							// so do nothing
						} else
							addError(insn, UNEXPECTED_ERROR);
					} else if (owner.equals(className)) {
						// call on the contract itself
						if (mi.name.equals("getCurrentTx")) {
							stack.pollLast(); // remove the "this" from stack
							pushVar(m, lastTxReceived);
							useLastTx = true;
						} else if (mi.name.equals("getCurrentBalance")) {
							stack.pollLast(); // remove the "this" from stack
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Current_Balance);
							code.putInt(tmpVar1);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getTxAfterTimestamp")) {
							arg1 = popVar(m, tmpVar1, false); // timestamp
							stack.pollLast(); // remove the "this" from stack

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.A_To_Tx_After_Timestamp);
							code.putInt(arg1.address);

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_A1);
							code.putInt(tmpVar2);
							pushVar(m, tmpVar2);
						} else if (mi.name.equals("parseAddress")) {
							StackVar address = stack.pollLast();
							stack.pollLast(); // remove the "this" from stack

							long value = 0;
							try {
								BurstCrypto bc = BurstCrypto.getInstance();
								// Decode without the BURST- prefix
								BurstID ad = bc.rsDecode(address.svalue.substring(6));
								value = ad.getSignedLongId();
							} catch (IllegalArgumentException ex) {
								addError(mi, ex.getMessage());
							}

							code.put(OpCode.e_op_code_SET_VAL);
							code.putInt(tmpVar1);
							code.putLong(value);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getCreator")) {
							stack.pollLast(); // remove the "this" from stack

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.B_To_Address_Of_Creator);

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_B1);
							code.putInt(tmpVar1);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getCreationTimestamp")) {
							stack.pollLast(); // remove the "this" from stack

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Creation_Timestamp);
							code.putInt(tmpVar1);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getBlockTimestamp")) {
							stack.pollLast(); // remove the "this" from stack

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Block_Timestamp);
							code.putInt(tmpVar1);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getPrevBlockHash")) {
							stack.pollLast(); // remove the "this" from stack

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Put_Last_Block_Hash_In_A);

							// Load the 4 register values
							for (int i = 0; i < 4; i++) {
								code.put(OpCode.e_op_code_EXT_FUN_RET);
								code.putShort((short) (OpCode.Get_A1 + 1));
								code.putInt(tmpVar1);
								pushVar(m, tmpVar1);
							}

						} else if (mi.name.equals("getPrevBlockTimestamp")) {
							stack.pollLast(); // remove the "this" from stack

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Last_Block_Timestamp);
							code.putInt(tmpVar1);

							pushVar(m, tmpVar1);
						} else if (mi.name.equals("sleep")) {
							arg1 = popVar(m, tmpVar1, false);
							stack.pollLast(); // remove the "this" from stack

							code.put(OpCode.e_op_code_SLP_DAT);
							code.putInt(tmpVar1);
						} else if (mi.name.equals("sendAmount")) {
							arg1 = popVar(m, tmpVar1, false); // address
							arg2 = popVar(m, tmpVar2, false); // amount
							stack.pollLast(); // remove the 'this'

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_B1);
							code.putInt(arg1.address); // address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Send_To_Address_In_B);
							code.putInt(arg2.address); // amount
						} else if (mi.name.equals("sendBalance")) {
							StackVar address = stack.pollLast();
							stack.pollLast(); // remove the 'this'

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_B1);
							code.putInt(address.address);

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Send_All_To_Address_In_B);
						} else if (mi.name.equals("sendMessage")) {
							arg1 = popVar(m, tmpVar1, false); // address
							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_B1);
							code.putInt(arg1.address); // address

							if (mi.desc.equals("(Ljava/lang/String;Lbt/Address;)V")) {
								// It should be a constant string, fill A1-A4 with 4*longs
								StackVar msg = stack.pollLast();
								int pos = 0;
								for (int a = 0; a < 4; a++) {
									long value = 0;
									for (int i = 0; i < 8; i++, pos++) {
										if (pos >= msg.svalue.length())
											break;
										long c = msg.svalue.charAt(pos);
										c <<= 8 * i;
										value += c;
									}
									code.put(OpCode.e_op_code_SET_VAL);
									code.putInt(tmpVar1);
									code.putLong(value);

									code.put(OpCode.e_op_code_EXT_FUN_DAT);
									code.putShort((short) (OpCode.Set_A1 + a));
									code.putInt(tmpVar1);
								}
							} else {
								// We should have received a Register, it is on stack
								for (int i = 3; i >= 0; i--) {
									StackVar reg = popVar(m, tmpVar1, false);
									code.put(OpCode.e_op_code_EXT_FUN_DAT);
									code.putShort((short) (OpCode.Set_A1 + i));
									code.putInt(reg.address);
								}
							}

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Send_A_To_Address_In_B);

							stack.pollLast(); // remove the 'this'
						} else {
							// check for user defined methods
							stack.pollLast(); // remove the 'this'
							Method mcall = methods.get(mi.name);
							if (mcall == null) {
								addError(mi, "Method not found: " + mi.name);
								return;
							}

							// update the local variable start position to not conflict with this one
							if (m.node.maxLocals > 0) {
								code.put(OpCode.e_op_code_SET_VAL);
								code.putInt(tmpVar1);
								code.putLong(m.node.maxLocals);
								code.put(OpCode.e_op_code_ADD_DAT);
								code.putInt(localStart);
								code.putInt(tmpVar1);
							}

							// load the arguments as local variables, tmpVar2 is the index
							for (int i = 0; i < mcall.nargs; i++) {
								if (i == 0) {
									code.put(OpCode.e_op_code_SET_DAT);
									code.putInt(tmpVar2);
									code.putInt(localStart);
								}

								StackVar argi = popVar(m, tmpVar1, false);
								code.put(OpCode.e_op_code_IND_DAT);
								code.putInt(tmpVar2);
								code.putInt(argi.address);

								// increment the local postion, not needed if this is the last argument
								for (int j = 0; i < mcall.nargs - 1 && j < mcall.localArgSize[i]; j++) {
									code.put(OpCode.e_op_code_INC_DAT);
									code.putInt(tmpVar2);
								}
							}

							// call method here
							code.put(OpCode.e_op_code_JMP_SUB);
							m.jumps.add(new Method.Jump(code.position(), mcall));
							code.putInt(0); // address, to be resolved latter

							// update the local variable start position back
							if (m.node.maxLocals > 0) {
								code.put(OpCode.e_op_code_SET_VAL);
								code.putInt(tmpVar1);
								code.putLong(m.node.maxLocals);
								code.put(OpCode.e_op_code_SUB_DAT);
								code.putInt(localStart);
								code.putInt(tmpVar1);
							}

							// check if the method has a return value
							if (!mcall.node.desc.endsWith("V")) {
								stack.add(new StackVar(STACK_PUSH, 0));
							}
						}
					} else if (owner.equals(Transaction.class.getName())) {
						// call on a transaction object
						if (mi.name.equals("getSenderAddress")) {
							arg1 = popVar(m, tmpVar1, false); // the TX we want the address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(arg1.address);

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.B_To_Address_Of_Tx_In_A);

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_B1);
							code.putInt(tmpVar1);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getAmount")) {
							arg1 = popVar(m, tmpVar1, false); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(arg1.address); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Amount_For_Tx_In_A);
							code.putInt(tmpVar1); // the amount
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getTimestamp")) {
							arg1 = popVar(m, tmpVar1, false); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(arg1.address); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Timestamp_For_Tx_In_A);
							code.putInt(tmpVar1); // the timestamp
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getMessage")) {
							arg1 = popVar(m, tmpVar1, false); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(arg1.address); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Message_From_Tx_In_A_To_B);

							// we push the four longs to the stack, so the field that receive
							// this should consume all of them
							for (int i = 0; i < 4; i++) {
								code.put(OpCode.e_op_code_EXT_FUN_RET);
								code.putShort((short) (OpCode.Get_B1 + i));
								code.putInt(tmpVar1); // the message contents
								pushVar(m, tmpVar1);
							}
						} else {
							addError(insn, UNEXPECTED_ERROR);
						}
					} else if (owner.equals(Object.class.getName())) {
						if (mi.name.equals("equals")) {
							arg1 = popVar(m, tmpVar1, true); // the obj 1
							arg2 = popVar(m, tmpVar2, false); // the obj 2

							code.put(OpCode.e_op_code_SUB_DAT);
							code.putInt(arg1.address);
							code.putInt(arg2.address);

							code.put(OpCode.e_op_code_CLR_DAT);
							code.putInt(tmpVar2);
							code.put(OpCode.e_op_code_BNZ_DAT);
							code.putInt(tmpVar1);
							code.put((byte) 0x0b); // offset

							code.put(OpCode.e_op_code_INC_DAT);
							code.putInt(tmpVar2);
							pushVar(m, tmpVar2);
						} else {
							addError(insn, UNEXPECTED_ERROR);
						}
					} else if (owner.equals(Register.class.getName())) {
						StackVar values[] = new StackVar[4];
						// we should pop the 4 values from stack
						for (int i = values.length - 1; i >= 0; i--) {
							values[i] = popVar(m, tmpVar1 + i, false);
						}
						if (mi.name.startsWith("getValue")) {
							int pos = Integer.parseInt(mi.name.substring(mi.name.length() - 1)) - 1;
							pushVar(m, values[pos].address);
						} else
							addError(insn, "Method not implemented: " + mi.name);
					} else {
						addError(insn, "Class not implemented: " + mi.owner);
					}
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}
				break;

			case GETFIELD:
			case PUTFIELD:
				if (insn instanceof FieldInsnNode) {
					FieldInsnNode fi = (FieldInsnNode) insn;

					System.out.println((opcode == GETFIELD ? "get " : "put ") + "field: " + fi.name);

					Field field = fields.get(fi.name);
					if (opcode == GETFIELD) {
						stack.pollLast(); // remove the 'this'
						for (int i = 0; i < field.size; i++) {
							pushVar(m, field.address + i);
						}
					} else {
						// PUTFIELD
						for (int i = field.size - 1; i >= 0; i--) {
							popVar(m, field.address + i, true);
						}
						stack.pollLast(); // remove the 'this'
					}
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}
				break;

			case LDC: // push a constant #index from a constant pool (String, int, float, Class,
						// java.lang.invoke.MethodType, or java.lang.invoke.MethodHandle) onto the stack
				if (insn instanceof LdcInsnNode) {
					LdcInsnNode ld = (LdcInsnNode) insn;
					System.out.println("constant: " + ld.cst);

					if (ld.cst instanceof String) {
						stack.addLast(new StackVar(STACK_CONSTANT, ld.cst));
					} else {
						long value = 0;
						if (ld.cst instanceof Long)
							value = (Long) ld.cst;
						else if (ld.cst instanceof Integer)
							value = (Integer) ld.cst;
						else {
							addError(ld, "Invalid constant: " + ld.cst);
						}
						code.put(OpCode.e_op_code_SET_VAL);
						code.putInt(tmpVar1);
						code.putLong(value);
						pushVar(m, tmpVar1);
					}
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}
				break;

			case LCMP: // push 0 if the two longs are the same, 1 if value1 is greater than value2, -1
						// otherwise
				arg2 = popVar(m, tmpVar2, false);
				arg1 = popVar(m, tmpVar1, true);
				code.put(OpCode.e_op_code_SUB_DAT);
				code.putInt(arg1.address);
				code.putInt(arg2.address);
				pushVar(m, tmpVar1);

				System.out.println("lcmp");
				break;

			case IFEQ:
			case IFNE:
			case IFGE:
			case IFGT:
			case IFLE:
			case IFLT:
			case IFNULL:
			case IFNONNULL:
			case GOTO:
				if (insn instanceof JumpInsnNode) {
					JumpInsnNode jmp = (JumpInsnNode) insn;

					arg1 = null;
					if (opcode != GOTO) {
						arg1 = popVar(m, tmpVar1, false);
					}

					// The idea is to branch on the negative of the command to skip
					// the JMP_ADR command, BZR and similar commands cannot be used
					// since the offset has only one byte (limite to -127 to 127).
					switch (opcode) {
					case IFEQ:
					case IFNULL:
						code.put(OpCode.e_op_code_BNZ_DAT);
						code.putInt(arg1.address);
						code.put((byte) 11);
						break;

					case IFNE:
					case IFNONNULL:
						code.put(OpCode.e_op_code_BZR_DAT);
						code.putInt(arg1.address);
						code.put((byte) 11);
						break;

					case IFGE:
					case IFGT:
						code.put(OpCode.e_op_code_CLR_DAT);
						code.putInt(tmpVar2);
						code.put(opcode == IFGE ? OpCode.e_op_code_BLT_DAT : OpCode.e_op_code_BLE_DAT);
						code.putInt(arg1.address);
						code.putInt(tmpVar2);
						code.put((byte) 15);
						break;
					case IFLE:
					case IFLT:
						code.put(OpCode.e_op_code_CLR_DAT);
						code.putInt(tmpVar2);
						code.put(opcode == IFLE ? OpCode.e_op_code_BGT_DAT : OpCode.e_op_code_BGE_DAT);
						code.putInt(arg1.address);
						code.putInt(tmpVar2);
						code.put((byte) 15);
						break;
					case GOTO:
						// do nothing, simply jump to the address
						break;
					}

					code.put(OpCode.e_op_code_JMP_ADR);
					m.jumps.add(new Method.Jump(code.position(), jmp.label));
					code.putInt(0); // address, to be resolved later

					System.out.println("ifeq: " + jmp.label.getLabel());
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}
				break;

			case IF_ACMPEQ:
			case IF_ACMPNE:
			case IF_ICMPEQ:
			case IF_ICMPNE:
				if (insn instanceof JumpInsnNode) {
					JumpInsnNode jmp = (JumpInsnNode) insn;

					arg1 = popVar(m, tmpVar1, true);
					arg2 = popVar(m, tmpVar2, false);

					code.put(OpCode.e_op_code_SUB_DAT);
					code.putInt(arg1.address);
					code.putInt(arg2.address);

					code.put(opcode == IF_ACMPEQ || opcode == IF_ICMPEQ ? OpCode.e_op_code_BNZ_DAT
							: OpCode.e_op_code_BZR_DAT);
					code.putInt(arg1.address);
					code.put((byte) 11); // offset

					code.put(OpCode.e_op_code_JMP_ADR);
					m.jumps.add(new Method.Jump(code.position(), jmp.label));
					code.putInt(0); // to be resolved later

					System.out.println("ifeq: " + jmp.label.getLabel());
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}
				break;

			default:
				addError(insn, "Unsuported opcode: " + opcode);
				break;
			}
		}
	}

	public ArrayList<Error> getErrors() {
		return errors;
	}

	void addError(AbstractInsnNode node, String error) {
		// try to find a line number to report
		int line = -1;
		AbstractInsnNode prev = node;
		while (prev != null) {
			if (prev instanceof LineNumberNode) {
				line = ((LineNumberNode) prev).line;
				break;
			}
			prev = prev.getPrevious();
		}
		String message = "line " + line + ": " + error;

		errors.add(new Error(node, message));
	}

	public static long getMethodSignature(Method m) {
		MessageDigest sha256 = new SHA256.Digest(); // TODO use burstkit4j, need to update
		return BurstCrypto.getInstance()
				.hashToId(sha256.digest((m.node.name + m.node.desc).getBytes(StandardCharsets.UTF_8)))
				.getSignedLongId();
	}

	public static String getMethodSignatureString(Method m) {
		return m.node.name + m.node.desc;
	}

	private static Pattern allParamsPattern = Pattern.compile("(\\(.*?\\))");
	private static Pattern paramsPattern = Pattern.compile("(\\[?)(C|Z|S|I|J|F|D|(:?L[^;]+;))");
	private static ArrayList<String> supportedParams = new ArrayList<>();
	static {
		supportedParams.add("Z");
		supportedParams.add("I");
		supportedParams.add("J");
		supportedParams.add("L" + Address.class.getName().replace('.', '/') + ';');
		supportedParams.add("L" + Transaction.class.getName().replace('.', '/') + ';');
		supportedParams.add("L" + Timestamp.class.getName().replace('.', '/') + ';');
	}

	void countMethodParameters(Method method) {
		Matcher m = allParamsPattern.matcher(method.node.desc);
		if (!m.find()) {
			throw new IllegalArgumentException("Method signature does not contain parameters");
		}
		String paramsDescriptor = m.group(1);
		Matcher mParam = paramsPattern.matcher(paramsDescriptor);

		method.nargs = 0;
		method.localArgTotal = 0;
		while (mParam.find()) {
			String p = mParam.group();
			if (!supportedParams.contains(p)) {
				addError(method.node.instructions.getFirst(), "Unsupported parameter type " + p);
			}
			if (method.nargs < Method.MAX_ARGS) {
				int argSize = p.equals("J") ? 2 : 1;
				method.localArgSize[method.nargs] = argSize;
				method.localArgPos[method.nargs] = method.localArgTotal;
				method.localArgTotal += argSize;
			}
			method.nargs++;
		}
	}
}
