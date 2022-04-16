package bt.compiler;

import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bt.*;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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


/**
 * Class to convert a {@link Contract} java bytecode to ciyam bytecode.
 * 
 * @author jjos
 */
public class Compiler {

	public static final CompilerVersion currentVersion = CompilerVersion.v0_0_0;

	public static final String INIT_METHOD = "<init>";
	public static final String MAIN_METHOD = "main";
	public static final String STARTED_METHOD = "blockStarted";
	public static final String FINISHED_METHOD = "blockFinished";
	public static final String TX_RECEIVED_METHOD = "txReceived";
	public static final int PAGE_SIZE = 256;

	private static final String UNEXPECTED_ERROR = "Unexpected error, please report at https://github.com/signum-network/signum-smartj/issues";
	
	private static Logger logger = LogManager.getLogger();

	ClassNode cn;
	AbstractInsnNode insn;
	ByteBuffer code;

	LinkedList<StackVar> stack = new LinkedList<>();
	StackVar pendingPush;

	HashMap<String, Method> methods = new HashMap<>();
	HashMap<String, Field> fields = new HashMap<>();
	HashMap<LabelNode, Integer> labels = new HashMap<>();

	String className;

	int lastFreeVar;
	int lastTxReceived;
	int lastTxTimestamp;
	int lastTxSender;
	int lastTxAmount;
	int tmpVar1, tmpVar2, tmpVar3, tmpVar4, tmpVar5, tmpVar6;
	int localStart;
	boolean useLocal;
	int creator;
	boolean useCreator;

	/** If we have public methods other than txReceived */
	boolean hasPublicMethods;
	boolean hasTxReceived;

	public class Error extends Throwable {
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
			logger.warn("WARNING: Target compiler version not specified");
		} else if (targetCompilerVersion.value() != currentVersion) {
			if (targetCompilerVersion.value().ordinal() > currentVersion.ordinal())
				logger.error(
						"WARNING: Target compiler version newer than compiler version. Newer features may not compile or work.");
			if (targetCompilerVersion.value().ordinal() < currentVersion.ordinal())
				logger.error(
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

	/**
	 * @return the compiled code byte array
	 */
	public byte[] getCode() {
		byte[] ret = new byte[code.position()];
		System.arraycopy(code.array(), 0, ret, 0, ret.length);
		return ret;
	}

	/**
	 * @return the number of pages occupied by this contract
	 */
	public int getCodeNPages() {
		return code.position() / PAGE_SIZE + 1;
	}

	public int getDataPages() {
		// check if this is actually enough
		int nvars = localStart + 3;
		int npages = nvars / 32 + 1;
		return npages;
	}

	public String getClassName() {
		return className;
	}

	private void readFields() {

		if (!cn.superName.replace('/', '.').equals(Contract.class.getName())) {
			addError(null, "A contract should derive from " + Contract.class.getName());
		}

		errors.clear();
		lastFreeVar = 0;
		useLocal = false;
		useCreator = false;

		for (FieldNode f : cn.fields) {
			logger.debug("field name: {}", f.name);
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

		// Variables reserved for the last tx information
		lastTxAmount = lastFreeVar++;
		lastTxSender = lastFreeVar++;

		// Temporary variables come last (used for pushing and poping values from user
		// stack)
		tmpVar1 = lastFreeVar++;
		tmpVar2 = lastFreeVar++;
		tmpVar3 = lastFreeVar++;
		tmpVar4 = lastFreeVar++;
		tmpVar5 = lastFreeVar++;
		tmpVar6 = lastFreeVar++;

		creator = lastFreeVar++;
		localStart = lastFreeVar++;
	}

	public void compile() {
		readFields();
		readMethods();		
	}

	private void initialCode() {
		// set the local variables start position
		if (useLocal) {
			code.put(OpCode.e_op_code_SET_VAL);
			code.putInt(localStart);
			code.putLong(lastFreeVar);
		}
		if (useCreator) {
			code.put(OpCode.e_op_code_EXT_FUN);
			code.putShort(OpCode.B_To_Address_Of_Creator);
			code.put(OpCode.e_op_code_EXT_FUN_RET);
			code.putShort(OpCode.Get_B1);
			code.putInt(creator);
		}

		// add the jump for the constructor
		Method initMethod = methods.get(INIT_METHOD);
		if (initMethod.code.position() > 1) {
			// only if we actually have a construction (it is not just the RET command)
			code.put(OpCode.e_op_code_JMP_SUB);
			code.putInt(methods.get(INIT_METHOD).address);
		}

		// The starting point for future calls (PCS)
		code.put(OpCode.e_op_code_SET_PCS);
		int afterPCSAddress = code.position();
		// Check if we have a blockStarted method and put it here
		Method startedMethod = getMethod(STARTED_METHOD);
		boolean hasStarted = startedMethod != null && startedMethod.code.position() > 1;
		if (hasStarted) {
			code.put(OpCode.e_op_code_JMP_SUB);
			code.putInt(startedMethod.address);
		}

		// Point to restart for a new transaction
		int afterBlockStartedAddress = code.position();

		if (hasPublicMethods || hasTxReceived) {
			// put the last transaction received in A (after the last timestamp)
			code.put(OpCode.e_op_code_EXT_FUN_DAT);
			code.putShort(OpCode.A_To_Tx_After_Timestamp);
			code.putInt(lastTxTimestamp);
			
			// get the value from A1
			code.put(OpCode.e_op_code_EXT_FUN_RET);
			code.putShort(OpCode.Get_A1);
			code.putInt(lastTxReceived);
		}

		// if zero we will FINISH (after the blockFinish method), otherwise continue
		Method finishMethod = getMethod(FINISHED_METHOD);
		boolean hasFinish = finishMethod != null && finishMethod.code.position() > 1;
		if (hasPublicMethods || hasTxReceived) {
			code.put(OpCode.e_op_code_BNZ_DAT);
			code.putInt(lastTxReceived);
			code.put((byte) (7 + (hasFinish ? 30 : 0)));
		}
		if (hasFinish) {
			code.put(OpCode.e_op_code_JMP_SUB);
			code.putInt(finishMethod.address);
			
			// Check if there is no new tx incoming.
			// This is in case we run out of balance during the finish method.
			code.put(OpCode.e_op_code_EXT_FUN_DAT);
			code.putShort(OpCode.A_To_Tx_After_Timestamp);
			code.putInt(lastTxTimestamp);
			
			// get the value from A1
			code.put(OpCode.e_op_code_EXT_FUN_RET);
			code.putShort(OpCode.Get_A1);
			code.putInt(tmpVar1);

			// If zero we finish, otherwise we restart
			code.put(OpCode.e_op_code_BZR_DAT);
			code.putInt(tmpVar1);
			code.put((byte) 11);
			
			code.put(OpCode.e_op_code_JMP_ADR);
			code.putInt(afterPCSAddress);
		}
		code.put(OpCode.e_op_code_FIN_IMD);

		if (hasPublicMethods || hasTxReceived) {
			// Store the timestamp of the last transaction
			code.put(OpCode.e_op_code_EXT_FUN_RET);
			code.putShort(OpCode.Get_Timestamp_For_Tx_In_A);
			code.putInt(lastTxTimestamp);
			// Get the sender of last transaction
			code.put(OpCode.e_op_code_EXT_FUN);
			code.putShort(OpCode.B_To_Address_Of_Tx_In_A);
			code.put(OpCode.e_op_code_EXT_FUN_RET);
			code.putShort(OpCode.Get_B1);
			code.putInt(lastTxSender);
			// Get the amount of last transaction
			code.put(OpCode.e_op_code_EXT_FUN_RET);
			code.putShort(OpCode.Get_Amount_For_Tx_In_A);
			code.putInt(lastTxAmount);
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
						|| m.node.name.equals(INIT_METHOD) || !Modifier.isPublic(m.node.access))
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
				code.put((byte) (16 + m.nargs * 7));

				// load the arguments on the local vars
				for (int i = 0; i < m.nargs; i++) {
					useLocal = true;
					code.put(OpCode.e_op_code_EXT_FUN_RET);
					code.putShort((short) (OpCode.Get_B1 + i + 1));
					code.putInt(localStart + 1 + m.localArgPos[i]);
				}
				// call the method
				code.put(OpCode.e_op_code_JMP_SUB);
				code.putInt(m.address);
				// end this run (check for the next transaction)
				code.put(OpCode.e_op_code_JMP_ADR);
				code.putInt(afterBlockStartedAddress);
			}
		}

		// call the txReceived method
		Method txReceivedMethod = methods.get(TX_RECEIVED_METHOD);
		if (txReceivedMethod.code.position() > 1) {
			// add method only if it is not empty (just the return command)
			code.put(OpCode.e_op_code_JMP_SUB);
			code.putInt(methods.get(TX_RECEIVED_METHOD).address);
		}

		if (hasPublicMethods || hasTxReceived) {
			// restart for a possible new transaction
			code.put(OpCode.e_op_code_JMP_ADR);
			code.putInt(afterBlockStartedAddress);
		}
	}

	public void link() {
		// we allow here a larger size, there will be an error when registering
		// if we pass the actual limit
		code = ByteBuffer.allocate(40 * Compiler.PAGE_SIZE);
		code.order(ByteOrder.LITTLE_ENDIAN);

		initialCode();
		int startMethodsPosition = code.position();

		// determine the address of each method
		int address = startMethodsPosition; // position of the first method
		for (Method m : methods.values()) {
			if (m.code.position() < 2)
				continue; // empty method
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
			if (m.code.position() < 2)
				continue; // empty method

			if (m.code.position() > code.capacity() - code.position()) {
				String methodList = "";
				for (Method mi : methods.values()) {
					methodList += "<br>" + mi.node.name + ", length: " + mi.code.position();
				}
				addError(m.node.instructions.get(0), "<html>Maximum AT code size exceeded:" + methodList);
				return;
			}
			code.put(m.code.array(), 0, m.code.position());
		}
	}

	private void readMethods() {
		hasPublicMethods = false;
		hasTxReceived = false;

		// First list all methods available
		for (MethodNode mnode : cn.methods) {
			if (mnode.name.equals(MAIN_METHOD))
				continue; // skip the main function (should be for deubgging only)

			Method m = new Method();
			m.node = mnode;
			countMethodParameters(m);
			m.hash = getMethodSignature(m);

			methods.put(mnode.name, m);

			if (!mnode.name.equals(TX_RECEIVED_METHOD) && !mnode.name.equals(INIT_METHOD)
					&& Modifier.isPublic(mnode.access)) {
				hasPublicMethods = true;
			}
		}

		if (errors.size() > 0)
			return;

		// Then parse
		for (Method m : methods.values()) {
			logger.debug("** METHOD: {}", m.node.name);
			if (m.hash != 0) {
				logger.info("METHOD: {}, hash: {}", m.node.name, m.hash);
			}
			try {
				parseMethod(m);
			} catch (Error e) {
				e.printStackTrace();
				addError(e);
			}

			if (m.node.name.equals(TX_RECEIVED_METHOD) && m.code.position() > 1)
				hasTxReceived = true;
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
	 * @param name
	 * @return the field address for the given name, -1 if not found
	 */
	public int getFieldAddress(String name) {
		Field f = fields.get(name);
		if (f != null)
			return f.address;
		return -1;
	}

	/**
	 * Push the variable on the given address to the stack.
	 */
	StackVar pushVar(Method m, int address) {
		/*
		 * TODO: we can try to optimize this later, recalling that no code can go
		 * between a pending push and its respective pop if (pendingPush != null) { //
		 * is a tmp var, not a field, push to AT stack
		 * m.code.put(OpCode.e_op_code_PSH_DAT); m.code.putInt(pendingPush.address);
		 * pendingPush = null; }
		 */

		StackVar v = new StackVar(address >= tmpVar1 ? STACK_PUSH : STACK_FIELD, address);
		stack.add(v);

		if (v.type == STACK_PUSH) {
			// pendingPush = v;
			// is a tmp var, not a field, push to AT stack
			m.code.put(OpCode.e_op_code_PSH_DAT);
			m.code.putInt(v.address);
			// pendingPush = null;
		}
		return v;
	}
	
	StackVar popVar(Method m, int destAddress, boolean forceCopy) throws Error {
		return popVar(m, destAddress, forceCopy, false);
	}

	/**
	 * Pop the lastest added variable from the stack and store on the given address.
	 * 
	 * @param m
	 * @param destAdd
	 * @throws Error ress
	 */
	StackVar popVar(Method m, int destAddress, boolean forceCopy, boolean alsoThis) throws Error {
		StackVar var = stack.pollLast();

		if (pendingPush != null && pendingPush.address == destAddress) {
			// simply do nothing, this is a push and pop on same variable
			pendingPush = null;
			return var;
		}
		if (pendingPush != null) {
			// execute the pending push
			m.code.put(OpCode.e_op_code_PSH_DAT);
			m.code.putInt(pendingPush.address);
			pendingPush = null;
		}

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
			if(!alsoThis) {
				throw getError(this.insn, UNEXPECTED_ERROR);
			}
		} else {
			throw getError(this.insn, UNEXPECTED_ERROR);
		}
		return var;
	}
	
	void popThis() throws Error {
		StackVar var = stack.pollLast();
		if (var.type != STACK_THIS) {
			throw getError(this.insn, UNEXPECTED_ERROR);
		}
	}

	private void parseMethod(Method m) throws Error {
		ByteBuffer code = ByteBuffer.allocate(40 * Compiler.PAGE_SIZE);
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

		StackVar arg1, arg2, arg3, arg4;

		Iterator<AbstractInsnNode> ite = m.node.instructions.iterator();
		while (ite.hasNext()) {
			insn = ite.next();

			int opcode = insn.getOpcode();
			checkNotUnsupported(opcode);

			if (opcode == -1) {
				// This is a label or line number information
				if (insn instanceof LabelNode) {
					LabelNode ln = (LabelNode) insn;
					labels.put(ln, code.position());
					logger.debug("label: {}", ln.getLabel());
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
				logger.debug("Stack");
				for (StackVar var : stack) {
					logger.debug(": " + var.toString());
				}
			}

			switch (opcode) {
			case NOP:
				break;
			case ILOAD:
			case LLOAD:
			case ALOAD:
				if (insn instanceof VarInsnNode) {
					VarInsnNode vi = (VarInsnNode) insn;
					if (vi.var > 0) {
						useLocal = true;
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
					logger.debug((opcode < ISTORE ? "load" : "store") + " local: " + vi.var);
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
					logger.debug("store local: " + vi.var);

					// tmpVar2 have the local index, starting at localStart
					useLocal = true;
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

			case ICONST_M1:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
				code.put(OpCode.e_op_code_SET_VAL);
				code.putInt(tmpVar2);
				code.putLong(opcode - ICONST_0);

				pushVar(m, tmpVar2);
				logger.debug("iconstant : " + (opcode - ICONST_0));
				break;

			case LCONST_1:
				code.put(OpCode.e_op_code_SET_VAL);
				code.putInt(tmpVar2);
				code.putLong(opcode - LCONST_0);

				pushVar(m, tmpVar2);
				logger.debug("lconstant : " + (opcode - LCONST_0));
				break;

			case ACONST_NULL:
			case ICONST_0:
			case LCONST_0:
				code.put(OpCode.e_op_code_CLR_DAT);
				code.putInt(tmpVar2);

				pushVar(m, tmpVar2);
				logger.debug("load null/zero");
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
			case IOR:
			case LOR:
			case IXOR:
			case LXOR:
				// we should have two arguments on the stack
				arg2 = popVar(m, tmpVar2, false);
				arg1 = popVar(m, tmpVar1, true);

				switch (opcode) {
				case ISUB:
				case LSUB:
					logger.debug("sub");
					code.put(OpCode.e_op_code_SUB_DAT);
					break;
				case IMUL:
				case LMUL:
					logger.debug("mul");
					code.put(OpCode.e_op_code_MUL_DAT);
					break;
				case IDIV:
				case LDIV:
					logger.debug("div");
					code.put(OpCode.e_op_code_DIV_DAT);
					break;
				case IREM:
				case LREM:
					logger.debug("mod");
					code.put(OpCode.e_op_code_MOD_DAT);
					break;
				case IAND:
				case LAND:
					logger.debug("AND");
					code.put(OpCode.e_op_code_AND_DAT);
					break;
				case IOR:
				case LOR:
					logger.debug("OR");
					code.put(OpCode.e_op_code_BOR_DAT);
					break;
				case IXOR:
				case LXOR:
					logger.debug("XOR");
					code.put(OpCode.e_op_code_XOR_DAT);
					break;
				default:
					logger.debug("add");
					code.put(OpCode.e_op_code_ADD_DAT);
					break;
				}
				code.putInt(arg1.address);
				code.putInt(arg2.address);

				pushVar(m, tmpVar1);
				break;
			case INEG:
			case LNEG:
				logger.debug("neg");

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
				arg1 = stack.pollLast();
				if (arg1.type != STACK_PUSH) {
					// add the pending push
					m.code.put(OpCode.e_op_code_PSH_DAT);
					m.code.putInt(arg1.address);
				}
			case RETURN:
				// Recalling that every method call will use JMP_SUB
				logger.debug("return");
				code.put(OpCode.e_op_code_RET_SUB);
				break;

			case DUP: // duplicate the value on top of the stack
			{
				StackVar var = popVar(m, tmpVar1, false, true);
				if (var.type == STACK_THIS) {
					stack.addLast(var);
					stack.addLast(var);
				} else if (var.type == STACK_PUSH) {
					pushVar(m, var.address);
					pushVar(m, var.address);
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}

				logger.debug("dup");
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

					logger.debug("invoke, name:" + mi.name + " owner:" + owner);

					if (owner.equals(Contract.class.getName())) {
						if (mi.name.equals(INIT_METHOD)) {
							// Contract super constructor call, do nothing
							popThis();
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
					} else if (owner.equals(Address.class.getName())) {
						// a call on the Address object
						if (mi.name.equals("getId")) {
							// it is the getId, the object itself is already on stack
							// so do nothing
						} else {
							addError(insn, UNEXPECTED_ERROR);
						}
					} else if (owner.equals(className)) {
						// call on the contract itself
						if (mi.name.equals("getCurrentTx")) {
							popThis();
							pushVar(m, lastTxReceived);
						} else if (mi.name.equals("getCurrentTxTimestamp")) {
							popThis();
							pushVar(m, lastTxTimestamp);
						} else if (mi.name.equals("getCurrentTxSender")) {
							popThis();
							pushVar(m, lastTxSender);
						} else if (mi.name.equals("getCurrentTxAmount")) {
							if(mi.desc.equals("(J)J")) {
								// asking for the creator of another contract
								if(!BT.isSIP37Activated())
									addError(insn, "activate SIP37 to support: " + mi.name);
								
								StackVar assetId = popVar(m, tmpVar1, false);
								popThis();
								
								code.put(OpCode.e_op_code_EXT_FUN_DAT_2);
								code.putShort(OpCode.Set_A1_A2);
								code.putInt(lastTxReceived);
								code.putInt(assetId.address);
								
								code.put(OpCode.e_op_code_EXT_FUN_RET);
								code.putShort(OpCode.Get_Amount_For_Tx_In_A);
								code.putInt(tmpVar1);

								pushVar(m, tmpVar1);
							}
							else {
								popThis();
								pushVar(m, lastTxAmount);
							}
						} else if (mi.name.equals("getCurrentBalance")) {
							if(mi.desc.equals("(J)J")) {
								// asking for the creator of another contract
								if(!BT.isSIP37Activated())
									addError(insn, "activate SIP37 to support: " + mi.name);
								
								StackVar assetId = popVar(m, tmpVar1, false);

								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort(OpCode.Set_B2);
								code.putInt(assetId.address);
							}
							else if(BT.isSIP37Activated()) {
								code.put(OpCode.e_op_code_EXT_FUN);
								code.putShort(OpCode.Clear_B);
							}
							
							popThis();
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Current_Balance);
							code.putInt(tmpVar1);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getTxAfterTimestamp")) {
							arg1 = popVar(m, tmpVar1, false); // timestamp
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.A_To_Tx_After_Timestamp);
							code.putInt(arg1.address);

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_A1);
							code.putInt(tmpVar2);
							pushVar(m, tmpVar2);
						} else if (mi.name.equals("parseAddress")) {
							StackVar address = stack.pollLast();
							popThis();

							long value = 0;
							try {
								SignumAddress ad = SignumAddress.fromRs(address.svalue);
								value = ad.getSignedLongId();
							} catch (IllegalArgumentException ex) {
								addError(mi, ex.getMessage());
							}

							code.put(OpCode.e_op_code_SET_VAL);
							code.putInt(tmpVar1);
							code.putLong(value);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getAddress")) {
							arg1 = popVar(m, tmpVar1, false); // the address
							popThis();
							
							pushVar(m, arg1.address);
						}
						else if (mi.name.equals("getCreator")) {
							if(mi.desc.equals("(Lbt/Address;)Lbt/Address;")) {
								// asking for the creator of another contract
								if(!BT.isSIP37Activated())
									addError(insn, "activate SIP37 to support: " + mi.name);
								
								StackVar otherContract = popVar(m, tmpVar1, false);
								popThis();

								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort(OpCode.Set_B2);
								code.putInt(otherContract.address);
								
								code.put(OpCode.e_op_code_EXT_FUN);
								code.putShort(OpCode.B_To_Address_Of_Creator);
								code.put(OpCode.e_op_code_EXT_FUN_RET);
								code.putShort(OpCode.Get_B1);
								code.putInt(tmpVar1);

								pushVar(m, tmpVar1);
							}
							else {
								popThis();

								useCreator = true;
								pushVar(m, creator);
							}
						}
						else if (mi.name.equals("getActivationFee")) {
							if(!BT.isSIP37Activated())
								addError(insn, "activate SIP37 to support: " + mi.name);
							if(mi.desc.equals("(Lbt/Address;)J")) {
								// asking for the activation fee of another contract
								
								StackVar otherContract = popVar(m, tmpVar1, false);

								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort(OpCode.Set_B2);
								code.putInt(otherContract.address);
							}
							else {
								// B2 must be clear to get the activation fee of this contract
								code.put(OpCode.e_op_code_EXT_FUN);
								code.putShort(OpCode.Clear_B);								
							}
							popThis();
							
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.GET_ACTIVATION_FEE);
							code.putInt(tmpVar1);

							pushVar(m, tmpVar1);
						}
						else if (mi.name.equals("calcPow")) {
							if(!BT.isSIP37Activated())
								addError(insn, "activate SIP37 to support: " + mi.name);

							// long calcPow(long x, long y)
							StackVar y = popVar(m, tmpVar2, false);
							StackVar x = popVar(m, tmpVar1, true);
							popThis();
							
							code.put(OpCode.e_op_code_POW_DAT);
							code.putInt(x.address);
							code.putInt(y.address);

							pushVar(m, x.address);
						}
						else if (mi.name.equals("calcMultDiv")) {
							if(!BT.isSIP37Activated())
								addError(insn, "activate SIP37 to support: " + mi.name);

							// long calcMultDiv(long x, long y, long den)
							StackVar den = popVar(m, tmpVar3, false);
							StackVar y = popVar(m, tmpVar2, false);
							StackVar x = popVar(m, tmpVar1, true);
							popThis();
							
							code.put(OpCode.e_op_code_MDV_DAT);
							code.putInt(x.address);
							code.putInt(y.address);
							code.putInt(den.address);

							pushVar(m, x.address);
						}
						else if (mi.name.equals("getCreationTimestamp")) {
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Creation_Timestamp);
							code.putInt(tmpVar1);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getBlockTimestamp")) {
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Block_Timestamp);
							code.putInt(tmpVar1);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getBlockHeight")) {
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Block_Timestamp);
							code.putInt(tmpVar1);
							
							// Get only the block height, removing the number of txs
							code.put(OpCode.e_op_code_SET_VAL);
							code.putInt(tmpVar2);
							code.putLong(32L);

							code.put(OpCode.e_op_code_SHR_DAT);
							code.putInt(tmpVar1);
							code.putInt(tmpVar2);
							
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getPrevBlockHash")) {
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Put_Last_Block_Hash_In_A);

							// Load the 4 register values
							for (int i = 0; i < 4; i++) {
								code.put(OpCode.e_op_code_EXT_FUN_RET);
								code.putShort((short) (OpCode.Get_A1 + i));
								code.putInt(tmpVar1);
								pushVar(m, tmpVar1);
							}
						} else if (mi.name.equals("getPrevBlockHash1")) {
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Put_Last_Block_Hash_In_A);

							// Load the first value
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort((short) (OpCode.Get_A1));
							code.putInt(tmpVar1);
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getPrevBlockTimestamp")) {
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Last_Block_Timestamp);
							code.putInt(tmpVar1);

							pushVar(m, tmpVar1);
						} else if (mi.name.equals("sleepOneBlock")) {
							popThis();
							code.put(OpCode.e_op_code_SLP_IMD);
						} else if (mi.name.equals("sleep")) {
							arg1 = popVar(m, tmpVar1, false);
							popThis();

							code.put(OpCode.e_op_code_SLP_DAT);
							code.putInt(arg1.address);
						} else if (mi.name.equals("issueAsset")) {
							if(!BT.isSIP37Activated())
								addError(insn, "activate SIP37 to support: " + mi.name);

							// issueAsset(long namePart1, long namePart2, long decimalPlaces)
							StackVar decimals = popVar(m, tmpVar3, false); // decimalPlaces
							StackVar namePart2 = popVar(m, tmpVar2, false); // namePart2
							StackVar namePart1 = popVar(m, tmpVar1, false); // namePart1
							popThis();
							
							code.put(OpCode.e_op_code_EXT_FUN_DAT_2);
							code.putShort(OpCode.Set_A1_A2);
							code.putInt(namePart1.address);
							code.putInt(namePart2.address);

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_B1);
							code.putInt(decimals.address);

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.ISSUE_ASSET);
							code.putInt(tmpVar1); // resulting asset id
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("mintAsset")) {
							if(!BT.isSIP37Activated())
								addError(insn, "activate SIP37 to support: " + mi.name);

							// mintAsset(long assetId, long quantity)
							StackVar quantity = popVar(m, tmpVar2, false); // quantity
							StackVar assetId = popVar(m, tmpVar1, false); // assetId
							popThis();
							
							code.put(OpCode.e_op_code_EXT_FUN_DAT_2);
							code.putShort(OpCode.Set_B1_B2);
							code.putInt(quantity.address);
							code.putInt(assetId.address);

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.MINT_ASSET);
						}
						else if (mi.name.equals("getAssetHoldersCount")) {
							if(!BT.isSIP37Activated())
								addError(insn, "activate SIP37 to support: " + mi.name);

							// getAssetHoldersCount(long minHolderAmount, long assetId) {
							StackVar assetId = popVar(m, tmpVar2, false);
							StackVar minHolderAmount = popVar(m, tmpVar1, false);
							popThis();
							
							code.put(OpCode.e_op_code_EXT_FUN_DAT_2);
							code.putShort(OpCode.Set_B1_B2);
							code.putInt(minHolderAmount.address);
							code.putInt(assetId.address);

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.ISSUE_ASSET);
							code.putInt(tmpVar1); // resulting counter
							pushVar(m, tmpVar1);
						}
						else if (mi.name.equals("distributeToHolders")) {
							if(!BT.isSIP37Activated())
								addError(insn, "activate SIP37 to support: " + mi.name);

							// void distributeToHolders(long minHolderAmount, long assetId, long amount, long assetToDistribute, long quantity)
							StackVar quantity = popVar(m, tmpVar5, false);
							StackVar assetToDistribute = popVar(m, tmpVar4, false);
							StackVar amount = popVar(m, tmpVar3, false);
							StackVar assetId = popVar(m, tmpVar2, false);
							StackVar minHolding = popVar(m, tmpVar1, false);
							popThis();
							
							code.put(OpCode.e_op_code_EXT_FUN_DAT_2);
							code.putShort(OpCode.Set_B1_B2);
							code.putInt(minHolding.address);
							code.putInt(assetId.address);

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(amount.address);

							code.put(OpCode.e_op_code_EXT_FUN_DAT_2);
							code.putShort(OpCode.Set_A3_A4);
							code.putInt(assetToDistribute.address);
							code.putInt(quantity.address);

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.DIST_TO_ASSET_HOLDERS);
						}
						else if (mi.name.equals("sendAmount")) {
							arg1 = popVar(m, tmpVar1, false); // address
							arg2 = popVar(m, tmpVar2, false); // amount
							if (mi.desc.equals("(JJLbt/Address;)V")) {
								if(!BT.isSIP37Activated())
									addError(insn, "activate SIP37 to support assets on: " + mi.name);

								// the version with the assetId
								arg3 = popVar(m, tmpVar3, false);
								
								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort(OpCode.Set_B2);
								code.putInt(arg3.address); // assetId
							}
							else if(BT.isSIP37Activated()) {
								// B2 must be clear for regular transfer
								code.put(OpCode.e_op_code_EXT_FUN);
								code.putShort(OpCode.Clear_B);
							}
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_B1);
							code.putInt(arg1.address); // address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Send_To_Address_In_B);
							code.putInt(arg2.address); // amount
						} else if (mi.name.equals("sendBalance")) {
							StackVar address = stack.pollLast();
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_B1);
							code.putInt(address.address);

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Send_All_To_Address_In_B);
						} else if (mi.name.equals("performSHA256_64")) {
							arg2 = popVar(m, tmpVar1, false); // input2
							arg1 = popVar(m, tmpVar2, false); // input1
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Clear_A);

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(arg1.address); // address
							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A2);
							code.putInt(arg2.address); // address

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.SHA256_A_To_B);

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_B1);
							code.putInt(tmpVar1); // resulting hash
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("performSHA256")) {
							arg4 = popVar(m, tmpVar1, false); // input4
							arg3 = popVar(m, tmpVar2, false); // input3
							arg2 = popVar(m, tmpVar3, false); // input2
							arg1 = popVar(m, tmpVar4, false); // input1
							popThis();

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(arg1.address); // address
							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A2);
							code.putInt(arg2.address); // address
							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A3);
							code.putInt(arg3.address); // address
							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A4);
							code.putInt(arg4.address); // address

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.SHA256_A_To_B);

							for (int i = 0; i < 4; i++) {
								code.put(OpCode.e_op_code_EXT_FUN_RET);
								code.putShort((short) (OpCode.Get_B1 + i));
								code.putInt(tmpVar1); // resulting hash
								pushVar(m, tmpVar1);
							}
						}
						else if (mi.name.equals("setMapValue")) {
							if(!BT.isSIP37Activated())
								addError(insn, "activate SIP37 to support: " + mi.name);
							
							StackVar value = popVar(m, tmpVar3, false);
							StackVar key2 = popVar(m, tmpVar2, false);
							StackVar key1 = popVar(m, tmpVar1, false);
							popThis();
							
							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Clear_A);
							
							code.put(OpCode.e_op_code_EXT_FUN_DAT_2);
							code.putShort((short) (OpCode.Set_A1_A2));
							code.putInt(key1.address);
							code.putInt(key2.address);
							
							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort((short) (OpCode.Set_A4));
							code.putInt(value.address);

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.SET_MAP_VALUE_KEYS_IN_A);
						}
						else if (mi.name.equals("getMapValue")) {
							if(!BT.isSIP37Activated())
								addError(insn, "activate SIP37 to support: " + mi.name);
							
							StackVar key2 = popVar(m, tmpVar3, false);
							StackVar key1 = popVar(m, tmpVar2, false);
							if (mi.desc.equals("(Lbt/Address;JJ)J")) {
								// long getMapValue(Address contract, long key1, long key2)
								StackVar address = popVar(m, tmpVar1, false);
								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort((short) (OpCode.Set_A3));
								code.putInt(address.address);
							}
							else {
								code.put(OpCode.e_op_code_EXT_FUN);
								code.putShort((short) (OpCode.Clear_A));
							}
							popThis();
							
							code.put(OpCode.e_op_code_EXT_FUN_DAT_2);
							code.putShort((short) (OpCode.Set_A1_A2));
							code.putInt(key1.address);
							code.putInt(key2.address);
							
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.GET_MAP_VALUE_KEYS_IN_A);
							code.putInt(tmpVar1);
							
							pushVar(m, tmpVar1);
						}
						else if (mi.name.equals("sendMessage")) {
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
							}
							else if (mi.desc.equals("(JLbt/Address;)V")) {
								// single long argument
								StackVar msg = popVar(m, tmpVar1, false);
								
								code.put(OpCode.e_op_code_EXT_FUN);
								code.putShort(OpCode.Clear_A);
								
								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort((short) (OpCode.Set_A1));
								code.putInt(msg.address);
							}
							else if (mi.desc.equals("(JJLbt/Address;)V")) {
								// two long arguments
								StackVar msg2 = popVar(m, tmpVar2, false);
								StackVar msg = popVar(m, tmpVar1, false);
								
								code.put(OpCode.e_op_code_EXT_FUN);
								code.putShort(OpCode.Clear_A);
								
								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort((short) (OpCode.Set_A1));
								code.putInt(msg.address);
								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort((short) (OpCode.Set_A2));
								code.putInt(msg2.address);
							}
							else if (mi.desc.equals("(JJJJLbt/Address;)V")) {
								// four long arguments
								StackVar msg4 = popVar(m, tmpVar4, false);
								StackVar msg3 = popVar(m, tmpVar3, false);
								StackVar msg2 = popVar(m, tmpVar2, false);
								StackVar msg = popVar(m, tmpVar1, false);
								
								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort((short) (OpCode.Set_A1));
								code.putInt(msg.address);
								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort((short) (OpCode.Set_A2));
								code.putInt(msg2.address);
								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort((short) (OpCode.Set_A3));
								code.putInt(msg3.address);
								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort((short) (OpCode.Set_A4));
								code.putInt(msg4.address);
							}
							else {
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

							popThis();
						} else {
							// check for user defined methods
							Method mcall = methods.get(mi.name);
							if (mcall == null) {
								addError(mi, "Method not found: " + mi.name);
								return;
							}

							// update the local variable start position to not conflict with this one
							if (m.node.maxLocals > 1) {
								useLocal = true;
								code.put(OpCode.e_op_code_SET_VAL);
								code.putInt(tmpVar1);
								code.putLong(m.node.maxLocals - 1);
								code.put(OpCode.e_op_code_ADD_DAT);
								code.putInt(localStart);
								code.putInt(tmpVar1);
							}

							// load the arguments as local variables, tmpVar2 is the index
							for (int i = 0; i < mcall.nargs; i++) {
								if (i == 0) {
									useLocal = true;
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
							popThis();

							// call method here
							code.put(OpCode.e_op_code_JMP_SUB);
							m.jumps.add(new Method.Jump(code.position(), mcall));
							code.putInt(0); // address, to be resolved latter

							// update the local variable start position back
							if (m.node.maxLocals > 1) {
								code.put(OpCode.e_op_code_SET_VAL);
								code.putInt(tmpVar1);
								code.putLong(m.node.maxLocals - 1);
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
							if(mi.desc.equals("(J)J")) {
								// asking for the token amount of a tx
								if(!BT.isSIP37Activated())
									addError(insn, "activate SIP37 to support: " + mi.name);
								
								StackVar assetId = popVar(m, tmpVar1, false);

								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort(OpCode.Set_A2);
								code.putInt(assetId.address);
							}
							else if(BT.isSIP37Activated()) {
								code.put(OpCode.e_op_code_EXT_FUN);
								code.putShort(OpCode.Clear_A);
							}

							arg1 = popVar(m, tmpVar1, false); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(arg1.address); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_Amount_For_Tx_In_A);
							code.putInt(tmpVar1); // the amount
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getId")) {
							// it is the getId, the object itself is already on stack
							// so do nothing
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
							if(mi.desc.equals("(Lbt/Address;J)Lbt/Register;")) {
								if(!BT.isSIP37Activated())
									addError(insn, "activate SIP37 to support: " + mi.name);
								
								StackVar page = popVar(m, tmpVar2, false);
								
								code.put(OpCode.e_op_code_EXT_FUN_DAT);
								code.putShort(OpCode.Set_A2);
								code.putInt(page.address);								
							}
							else if(BT.isSIP37Activated()) {
								code.put(OpCode.e_op_code_EXT_FUN);
								code.putShort(OpCode.Clear_A);
							}
							
							StackVar tx = popVar(m, tmpVar1, false);

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(tx.address);
							
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
						} else if (mi.name.equals("checkMessageSHA256")) {
							arg4 = popVar(m, tmpVar1, false); // input4
							arg3 = popVar(m, tmpVar2, false); // input3
							arg2 = popVar(m, tmpVar3, false); // input2
							arg1 = popVar(m, tmpVar4, false); // input1

							StackVar txArg = popVar(m, tmpVar5, false); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(txArg.address); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Message_From_Tx_In_A_To_B);
							
							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Copy_A_From_B);
							
							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_B1);
							code.putInt(arg1.address); // address
							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_B2);
							code.putInt(arg2.address); // address
							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_B3);
							code.putInt(arg3.address); // address
							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_B4);
							code.putInt(arg4.address); // address
							
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Check_SHA256_A_With_B);
							code.putInt(tmpVar1); // the check result
							
							pushVar(m, tmpVar1);
							
						} else if (mi.name.equals("checkMessageSHA256_192")) {
							arg4 = popVar(m, tmpVar4, false); // input4
							arg3 = popVar(m, tmpVar3, false); // input3
							arg2 = popVar(m, tmpVar2, false); // input2
							arg1 = popVar(m, tmpVar1, false); // input1

							StackVar txArg = popVar(m, tmpVar5, false); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(txArg.address); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Message_From_Tx_In_A_To_B);
							
							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Copy_A_From_B);
							
							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.SHA256_A_To_B);
							
							// tmpVar1 will be zero if match
							code.put(OpCode.e_op_code_CLR_DAT);
							code.putInt(tmpVar1);

							// check 2
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort((short) (OpCode.Get_B2));
							code.putInt(tmpVar5);
							code.put(OpCode.e_op_code_SUB_DAT);
							code.putInt(tmpVar5);
							code.putInt(arg2.address);
							code.put(OpCode.e_op_code_BNZ_DAT);
							code.putInt(tmpVar5);
							code.put((byte) 0x0b); // offset
							code.put(OpCode.e_op_code_INC_DAT);
							code.putInt(tmpVar1);
							
							// check 3
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort((short) (OpCode.Get_B3));
							code.putInt(tmpVar5);
							code.put(OpCode.e_op_code_SUB_DAT);
							code.putInt(tmpVar5);
							code.putInt(arg3.address);
							code.put(OpCode.e_op_code_BNZ_DAT);
							code.putInt(tmpVar5);
							code.put((byte) 0x0b); // offset
							code.put(OpCode.e_op_code_INC_DAT);
							code.putInt(tmpVar1);
							
							// check 4
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort((short) (OpCode.Get_B4));
							code.putInt(tmpVar5);
							code.put(OpCode.e_op_code_SUB_DAT);
							code.putInt(tmpVar5);
							code.putInt(arg4.address);
							code.put(OpCode.e_op_code_BNZ_DAT);
							code.putInt(tmpVar5);
							code.put((byte) 0x0b); // offset
							code.put(OpCode.e_op_code_INC_DAT);
							code.putInt(tmpVar1);
							
							// tmpVar1 is zero if match, so we return 1 if match
							code.put(OpCode.e_op_code_CLR_DAT);
							code.putInt(tmpVar2);
							code.put(OpCode.e_op_code_BNZ_DAT);
							code.putInt(tmpVar1);
							code.put((byte) 0x0b); // offset
							code.put(OpCode.e_op_code_INC_DAT);
							code.putInt(tmpVar2);
							
							pushVar(m, tmpVar2);
							
						} else if (mi.name.equals("getMessage1")) {
							arg1 = popVar(m, tmpVar1, false); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(arg1.address); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Message_From_Tx_In_A_To_B);

							// we push only the first long to the stack
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort(OpCode.Get_B1);
							code.putInt(tmpVar1); // the message contents
							pushVar(m, tmpVar1);
						} else if (mi.name.equals("getMessage2")) {
							arg1 = popVar(m, tmpVar1, false); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN_DAT);
							code.putShort(OpCode.Set_A1);
							code.putInt(arg1.address); // the TX address

							code.put(OpCode.e_op_code_EXT_FUN);
							code.putShort(OpCode.Message_From_Tx_In_A_To_B);

							// we push only the first long to the stack
							code.put(OpCode.e_op_code_EXT_FUN_RET);
							code.putShort((short) (OpCode.Get_B2));
							code.putInt(tmpVar1); // the message contents
							pushVar(m, tmpVar1);
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
							values[i] = popVar(m, tmpVar1 + i, true);
						}
						if (mi.name.startsWith("getValue")) {
							int pos = Integer.parseInt(mi.name.substring(mi.name.length() - 1)) - 1;
							pushVar(m, values[pos].address);
						}
						else if (mi.name.equals("equals")) {
							code.put(OpCode.e_op_code_CLR_DAT);
							code.putInt(tmpVar5);

							// we have another register on stack
							for (int i = values.length - 1; i >= 0; i--) {
								StackVar other = popVar(m, tmpVar6, false);

								code.put(OpCode.e_op_code_SUB_DAT);
								code.putInt(values[i].address);
								code.putInt(other.address);

								code.put(OpCode.e_op_code_BNZ_DAT);
								code.putInt(values[i].address);
								code.put((byte) 0x0b); // offset

								code.put(OpCode.e_op_code_INC_DAT);
								code.putInt(tmpVar5);
							}
							// tmpVar 5 must be equal 4
							code.put(OpCode.e_op_code_SET_VAL);
							code.putInt(tmpVar1);
							code.putLong(4L);

							code.put(OpCode.e_op_code_SUB_DAT);
							code.putInt(tmpVar5);
							code.putInt(tmpVar1);

							code.put(OpCode.e_op_code_CLR_DAT);
							code.putInt(tmpVar2);

							code.put(OpCode.e_op_code_BNZ_DAT);
							code.putInt(tmpVar5);
							code.put((byte) 0x0b); // offset

							code.put(OpCode.e_op_code_INC_DAT);
							code.putInt(tmpVar2);

							pushVar(m, tmpVar2);
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
			case GETSTATIC:
			case PUTSTATIC:
				if (insn instanceof FieldInsnNode) {
					FieldInsnNode fi = (FieldInsnNode) insn;

					logger.debug((opcode == GETFIELD ? "get " : "put ") + "field: " + fi.name);

					Field field = fields.get(fi.name);
					if (opcode == GETFIELD || opcode == GETSTATIC) {
						if(opcode == GETFIELD) {
							popThis();
						}
						for (int i = 0; i < field.size; i++) {
							pushVar(m, field.address + i);
						}
					} else {
						// PUTFIELD
						for (int i = field.size - 1; i >= 0; i--) {
							popVar(m, field.address + i, true);
						}
						if(opcode == PUTFIELD) {
							popThis();
						}
					}
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}
				break;
			
			case LDC: // push a constant #index from a constant pool (String, int, float, Class,
						// java.lang.invoke.MethodType, or java.lang.invoke.MethodHandle) onto the stack
				if (insn instanceof LdcInsnNode) {
					LdcInsnNode ld = (LdcInsnNode) insn;
					logger.debug("constant: " + ld.cst);

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
						code.putInt(tmpVar2);
						code.putLong(value);
						pushVar(m, tmpVar2);
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
				if(arg1.address == null || arg2.address == null) {
					addError(insn, UNEXPECTED_ERROR);
					return;
				}
				code.putInt(arg1.address);
				code.putInt(arg2.address);
				pushVar(m, arg1.address);

				logger.debug("lcmp");
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

					logger.debug("ifeq: " + jmp.label.getLabel());
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

					logger.debug("ifeq: " + jmp.label.getLabel());
				} else {
					addError(insn, UNEXPECTED_ERROR);
				}
				break;

			case POP:
				// discard the top value on the stack
			case POP2:
				// discard the top two values on the stack (or one value, if it is a double or
				// long)
				popVar(m, tmpVar1, false);
				break;

			default:
				addError(insn, "OpCode Not Implemented: " + opcode);
				break;
			}
		}
	}

	public ArrayList<Error> getErrors() {
		return errors;
	}

	void addError(AbstractInsnNode node, String error) {
		errors.add(getError(node, error));
	}
	
	void addError(Error error) {
		errors.add(error);
	}
	
	Error getError(AbstractInsnNode node, String error) {
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

		return new Error(node, message);
	}

	public static long getMethodSignature(Method m) {
		SignumCrypto burstCrypto = SignumCrypto.getInstance();
		MessageDigest sha256 = burstCrypto.getSha256();
		return burstCrypto.hashToId(sha256.digest((m.node.name + m.node.desc).getBytes(StandardCharsets.UTF_8)))
				.getSignedLongId(); // TODO replace
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

	private static final List<Integer> unsupportedOpcodes = Arrays.asList(
			ATHROW,
			CHECKCAST,
			INSTANCEOF,
			MONITORENTER,
			MONITOREXIT
	);

	private void checkNotUnsupported(int opcode) {
		if (unsupportedOpcodes.contains(opcode)) {
			throw new UnsupportedOperationException("OpCode " + opcode + " not supported");
		}
	}
}
