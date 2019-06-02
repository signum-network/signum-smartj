package bt.compiler;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Helper class for printing ciyam bytecode.
 * 
 * @author jjos
 */
public class Printer {

	private final static char[] hexArray = "0123456789abcdef".toCharArray();

	static int print(byte[] bytes, int start, int length, PrintStream out) {
		for (int j = length - 1; j >= 0; j--) {
			int v = bytes[j + start] & 0xff;
			print(v, out);
		}
		return length;
	}

	static void print(int v, PrintStream out) {
		if (v < 0) // handled signed value as unsigned
			v += 256;
		int c1 = v >>> 4;
		int c2 = v & 0x0F;
		out.print(hexArray[c1]);
		out.print(hexArray[c2]);
	}

	static int printAddress(byte[] code, int p, PrintStream out, Compiler c) {
		out.print(tab);
		int ret = print(code, p, 4, out);
		out.print(" address");
		if (c != null) {
			ByteBuffer address = ByteBuffer.allocate(4);
			address.order(ByteOrder.LITTLE_ENDIAN);
			for (int i = 0; i < 4; i++) {
				address.put(code[p + i]);
			}
			address.clear();
			int ad = address.getInt();

			// print the variable name here
			for (Field fi : c.fields.values()) {
				if (fi.address == ad) {
					out.print(" (" + fi.node.name + ")");
					break;
				}
			}
			if (ad == c.lastTxReceived)
				out.print(" (lastTxReceived)");
			else if (ad == c.lastTxTimestamp)
				out.print(" (lastTxTimestamp)");
			else if (ad == c.tmpVar1)
				out.print(" (tmpVar1)");
			else if (ad == c.tmpVar2)
				out.print(" (tmpVar2)");
			else if (ad == c.tmpVar3)
				out.print(" (tmpVar3)");
			else if (ad == c.tmpVar4)
				out.print(" (tmpVar4)");
			else if (ad == c.localStart)
				out.print(" (localStart)");
		}
		out.println();
		return ret;
	}

	static int printOp(byte[] bytes, int start, int length, PrintStream out) {
		// Print the address of this operation first, making it easier to inspect jumps
		out.print('@');
		print(start >>> 8, out);
		print(start & 0xff, out);
		out.print(' ');

		print(bytes, start, length, out);
		return length;
	}

	public static void printCode(ByteBuffer buf, PrintStream out) {
		int p = 0;
		byte[] code = buf.array();
		while (p < buf.position()) {
			print(code[p++], out);
		}
	}

	public static void print(ByteBuffer buf, PrintStream out, Compiler c) {
		print(buf.array(), buf.position(), out, c);
	}

	private static final String tab = "\t";

	public static void print(byte[] code, int length, PrintStream out, Compiler c) {

		ByteBuffer address = ByteBuffer.allocate(4);
		address.order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer value = ByteBuffer.allocate(8);
		value.order(ByteOrder.LITTLE_ENDIAN);

		int p = 0;
		while (p < length) {
			if (p > 0) {
				// check if this is the start position of a method
				for (Method m : c.getMethods()) {
					if (m.address == p)
						out.println("--> " + m.getName() + " method");
				}
			}

			int op = code[p];
			switch (op) {
			case OpCode.e_op_code_NOP:
				p += printOp(code, p, 1, out);
				out.println("\tNOP");
				break;
			case OpCode.e_op_code_SET_VAL:
				p += printOp(code, p, 1, out);
				out.println("\tSET_VAL");
				p += printAddress(code, p, out, c);
				out.print(tab);
				p += print(code, p, 8, out);
				out.println(" value");
				break;

			case OpCode.e_op_code_SET_DAT:
				p += printOp(code, p, 1, out);
				out.println("\tSET_DAT");
				p += printAddress(code, p, out, c);
				p += printAddress(code, p, out, c);
				break;
			case OpCode.e_op_code_CLR_DAT:
			case OpCode.e_op_code_INC_DAT:
			case OpCode.e_op_code_DEC_DAT:
			case OpCode.e_op_code_NOT_DAT:
				p += printOp(code, p, 1, out);
				switch (op) {
				case OpCode.e_op_code_CLR_DAT:
					out.println("\tCLR_DAT");
					break;
				case OpCode.e_op_code_INC_DAT:
					out.println("\tINC_DAT");
					break;
				case OpCode.e_op_code_DEC_DAT:
					out.println("\tDEC_DAT");
					break;
				case OpCode.e_op_code_NOT_DAT:
					out.println("\tNOT_DAT");
					break;
				default:
					out.println();
				}
				p += printAddress(code, p, out, c);
				break;
			case OpCode.e_op_code_PSH_DAT:
			case OpCode.e_op_code_POP_DAT:
				p += printOp(code, p, 1, out);
				out.println(op == OpCode.e_op_code_PSH_DAT ? "\tPSH_DAT" : "\tPOP_DAT");
				p += printAddress(code, p, out, c);
				break;
			case OpCode.e_op_code_JMP_SUB:
				p += printOp(code, p, 1, out);
				out.println("\tJMP_SUB");
				p += printAddress(code, p, out, null);
				break;
			case OpCode.e_op_code_JMP_ADR:
				p += printOp(code, p, 1, out);
				out.println("\tJMP_ADR");
				p += printAddress(code, p, out, null);
				break;
			case OpCode.e_op_code_ADD_DAT:
			case OpCode.e_op_code_SUB_DAT:
			case OpCode.e_op_code_MUL_DAT:
			case OpCode.e_op_code_DIV_DAT:
			case OpCode.e_op_code_BOR_DAT:
			case OpCode.e_op_code_AND_DAT:
			case OpCode.e_op_code_XOR_DAT:
			case OpCode.e_op_code_MOD_DAT:
				p += printOp(code, p, 1, out);
				switch (op) {
				case OpCode.e_op_code_ADD_DAT:
					out.println("\tADD_DAT");
					break;
				case OpCode.e_op_code_SUB_DAT:
					out.println("\tSUB_DAT");
					break;
				case OpCode.e_op_code_MUL_DAT:
					out.println("\tMUL_DAT");
					break;
				case OpCode.e_op_code_DIV_DAT:
					out.println("\tDIV_DAT");
					break;
				case OpCode.e_op_code_BOR_DAT:
					out.println("\tBOR_DAT");
					break;
				case OpCode.e_op_code_AND_DAT:
					out.println("\tAND_DAT");
					break;
				case OpCode.e_op_code_XOR_DAT:
					out.println("\tXOR_DAT");
					break;
				case OpCode.e_op_code_MOD_DAT:
					out.println("\tMOD_DAT");
					break;
				default:
					out.println();
				}
				p += printAddress(code, p, out, c);
				p += printAddress(code, p, out, c);
				break;
			case OpCode.e_op_code_SET_IND:
			case OpCode.e_op_code_IND_DAT:
				p += printOp(code, p, 1, out);
				out.println(op == OpCode.e_op_code_SET_IND ? "\tSET_IND" : "\tIND_DAT");
				p += printAddress(code, p, out, c);
				p += printAddress(code, p, out, c);
				break;

			case OpCode.e_op_code_SET_IDX:
			case OpCode.e_op_code_IDX_DAT:
				p += printOp(code, p, 1, out);
				out.println(op == OpCode.e_op_code_SET_IDX ? "\tSET_IDX" : "\tIDX_DAT");
				p += printAddress(code, p, out, c);
				p += printAddress(code, p, out, c);
				p += printAddress(code, p, out, c);
				break;

			case OpCode.e_op_code_RET_SUB:
				p += printOp(code, p, 1, out);
				out.println("\tRET_SUB");
				break;

			case OpCode.e_op_code_SET_PCS:
				p += printOp(code, p, 1, out);
				out.println("\tSET_PCS");
				break;
			case OpCode.e_op_code_EXT_FUN:
				p += printOp(code, p, 1, out);
				out.println("\tEXT_FUN");
				out.print(tab);
				p += print(code, p, 2, out);
				out.println(" " + funcName(code, p));
				break;
			case OpCode.e_op_code_EXT_FUN_DAT:
			case OpCode.e_op_code_EXT_FUN_RET:
				p += printOp(code, p, 1, out);
				out.println("\tEXT_FUN_" + (op == OpCode.e_op_code_EXT_FUN_DAT ? "DAT" : "RET"));
				out.print(tab);
				p += print(code, p, 2, out);
				out.println(" " + funcName(code, p));
				p += printAddress(code, p, out, c);
				break;
			case OpCode.e_op_code_EXT_FUN_DAT_2:
			case OpCode.e_op_code_EXT_FUN_RET_DAT:
				p += printOp(code, p, 1, out);
				out.println("\tEXT_FUN_RET_" + (op == OpCode.e_op_code_EXT_FUN_DAT_2 ? "DAT_2" : "RET_DAT"));
				out.print(tab);
				p += print(code, p, 2, out);
				out.println(" " + funcName(code, p));
				p += printAddress(code, p, out, c);
				p += printAddress(code, p, out, c);
				break;

			case OpCode.e_op_code_EXT_FUN_RET_DAT_2:
				p += printOp(code, p, 1, out);
				out.println("\tEXT_FUN_RET_DAT_2");
				out.print(tab);
				p += print(code, p, 2, out);
				out.println(" " + funcName(code, p));
				p += printAddress(code, p, out, c);
				p += printAddress(code, p, out, c);
				p += printAddress(code, p, out, c);
				break;

			case OpCode.e_op_code_BZR_DAT:
			case OpCode.e_op_code_BNZ_DAT:
				p += printOp(code, p, 1, out);
				out.println(op == OpCode.e_op_code_BZR_DAT ? "\tBZR" : "\tBNZ");
				p += printAddress(code, p, out, c);
				out.print(tab);
				p += print(code, p, 1, out);
				out.println(" offset");
				break;

			case OpCode.e_op_code_BGT_DAT:
			case OpCode.e_op_code_BLT_DAT:
			case OpCode.e_op_code_BGE_DAT:
			case OpCode.e_op_code_BLE_DAT:
			case OpCode.e_op_code_BEQ_DAT:
			case OpCode.e_op_code_BNE_DAT:
				p += printOp(code, p, 1, out);
				switch (op) {
				case OpCode.e_op_code_BGT_DAT:
					out.println("\tBGT");
					break;
				case OpCode.e_op_code_BLT_DAT:
					out.println("\tBLT");
					break;
				case OpCode.e_op_code_BGE_DAT:
					out.println("\tBGE");
					break;
				case OpCode.e_op_code_BLE_DAT:
					out.println("\tBLE");
					break;
				case OpCode.e_op_code_BEQ_DAT:
					out.println("\tBEQ");
					break;
				case OpCode.e_op_code_BNE_DAT:
					out.println("\tBNE");
					break;
				}
				p += printAddress(code, p, out, c);
				p += printAddress(code, p, out, c);
				out.print(tab);
				p += print(code, p, 1, out);
				out.println(" offset");
				break;

			case OpCode.e_op_code_SLP_DAT:
				p += printOp(code, p, 1, out);
				out.println("\tSLP_DAT");
				out.print(tab);
				p += print(code, p, 4, out);
				out.println(" address");
				break;

			case OpCode.e_op_code_FIN_IMD:
				p += printOp(code, p, 1, out);
				out.println("\tFIN");
				break;

			case OpCode.e_op_code_SHL_DAT:
			case OpCode.e_op_code_SHR_DAT:

			case OpCode.e_op_code_FIZ_DAT:
			case OpCode.e_op_code_STZ_DAT:
			case OpCode.e_op_code_STP_IMD:
			case OpCode.e_op_code_SLP_IMD:
			case OpCode.e_op_code_ERR_ADR:

			default:
				p += printOp(code, p, 1, out);
				out.println("\tUNSUPPORTED");
				break;
			}
		}
	}

	static String funcName(byte[] bytes, int end) {
		int v;
		v = bytes[end - 1] << 8;
		v += bytes[end - 2];

		switch (v) {
		case OpCode.Set_A1: // 0x0110; // EXT_FUN_DAT sets A1 from $addr
			return "Set_A1";
		case OpCode.Set_A2: // 0x0111; // EXT_FUN_DAT sets A2 from $addr
			return "Set_A2";
		case OpCode.Set_A3: // 0x0112; // EXT_FUN_DAT sets A3 from $addr
			return "Set_A3";
		case OpCode.Set_A4: // 0x0113; // EXT_FUN_DAT sets A4 from $addr
			return "Set_A4";
		case OpCode.Set_A1_A2: // 0x0114; // EXT_FUN_DAT_2 sets A1 from $addr1 and A2 from $addr2
			return "Set_A1_A2";
		case OpCode.Set_A3_A4: // 0x0115; // EXT_FUN_DAT_2 sets A3 from $addr1 and A4 from $addr2
			return "Set_A3_A4";
		case OpCode.Set_B1: // 0x0116; // EXT_FUN_DAT sets B1 from $addr
			return "Set_B1";
		case OpCode.Set_B2: // 0x0117; // EXT_FUN_DAT sets B2 from $addr
			return "Set_B2";
		case OpCode.Set_B3: // 0x0118; // EXT_FUN_DAT sets B3 from $addr
			return "Set_B3";
		case OpCode.Set_B4: // 0x0119; // EXT_FUN_DAT sets B4 from $addr
			return "Set_B4";
		case OpCode.Set_B1_B2: // 0x011a; // EXT_FUN_DAT_2 sets B1 from $addr1 and B2 from $addr2
			return "Set_B1_B2";
		case OpCode.Set_B3_B4: // 0x011b; // EXT_FUN_DAT_2 sets B3 from $addr1 and B4 from $addr2
			return "Set_B3_B4";

		case OpCode.Clear_A: // 0x0120; // EXT_FUN sets A to zero (A being A1..4)
			return "Clear_A";
		case OpCode.Clear_B: // 0x0121; // EXT_FUN sets B to zero (B being B1..4)
			return "Clear_B";
		case OpCode.Clear_A_And_B: // 0x0122; // EXT_FUN sets both A and B to zero
			return "Clear_A_And_B";
		case OpCode.Copy_A_From_B: // 0x0123; // EXT_FUN copies B into A
			return "Copy_A_From_B";
		case OpCode.Copy_B_From_A: // 0x0124; // EXT_FUN copies A into B
			return "Copy_B_From_A";
		case OpCode.Check_A_Is_Zero: // 0x0125; // EXT_FUN_RET @addr to 1 if A is zero or 0 if it is not (i.e. bool)
			return "Check_A_Is_Zero";
		case OpCode.Check_B_Is_Zero: // 0x0126; // EXT_FUN_RET @addr to 1 if B is zero of 0 if it is not (i.e. bool)
			return "Check_B_Is_Zero";
		case OpCode.Check_A_Equals_B: // 0x0127; // EXT_FUN_RET @addr to bool if A is equal to B
			return "Check_A_Equals_B";
		case OpCode.Swap_A_and_B: // 0x0128; // EXT_FUN swap the values of A and B
			return "Swap_A_and_B";
		case OpCode.OR_A_with_B: // 0x0129; // EXT_FUN sets A to A | B (bitwise OR)
			return "OR_A_with_B";
		case OpCode.OR_B_with_A: // 0x012a; // EXT_FUN sets B to B | A (bitwise OR)
			return "OR_B_with_A";
		case OpCode.AND_A_with_B: // 0x012b; // EXT_FUN sets A to A & B (bitwise AND)
			return "AND_A_with_B";
		case OpCode.AND_B_with_A: // 0x012c; // EXT_FUN sets B to B & A (bitwise AND)
			return "AND_B_with_A";
		case OpCode.XOR_A_with_B: // 0x012d; // EXT_FUN sets A to A ^ B (bitwise XOR)
			return "XOR_A_with_B";
		case OpCode.XOR_B_with_A: // 0x012e; // EXT_FUN sets B to B ^ A (bitwise XOR)
			return "XOR_B_with_A";

		case OpCode.Get_A1: // 0x0100; // EXT_FUN_RET sets @addr to A1
			return "Get_A1";
		case OpCode.Get_A2: // 0x0101; // EXT_FUN_RET sets @addr to A2
			return "Get_A2";
		case OpCode.Get_A3: // 0x0102; // EXT_FUN_RET sets @addr to A3
			return "Get_A3";
		case OpCode.Get_A4: // 0x0103; // EXT_FUN_RET sets @addr to A4
			return "Get_A4";
		case OpCode.Get_B1: // 0x0104; // EXT_FUN_RET sets @addr to B1
			return "Get_B1";
		case OpCode.Get_B2: // 0x0105; // EXT_FUN_RET sets @addr to B2
			return "Get_B2";
		case OpCode.Get_B3: // 0x0106; // EXT_FUN_RET sets @addr to B3
			return "Get_B3";
		case OpCode.Get_B4: // 0x0107; // EXT_FUN_RET sets @addr to B4
			return "Get_B4";

		case OpCode.Get_Block_Timestamp: // 0x0300; // EXT_FUN_RET sets @addr to the timestamp of the current block
			return "Get_Block_Timestamp";
		case OpCode.Get_Creation_Timestamp: // 0x0301; // EXT_FUN_RET sets @addr to the timestamp of the AT creation
											// block
			return "Get_Creation_Timestamp";
		case OpCode.Get_Last_Block_Timestamp: // 0x0302; // EXT_FUN_RET sets @addr to the timestamp of the previous
												// block
			return "Get_Last_Block_Timestamp";
		case OpCode.Put_Last_Block_Hash_In_A: // 0x0303; // EXT_FUN puts the block hash of the previous block in A
			return "Put_Last_Block_Hash_In_A";
		case OpCode.A_To_Tx_After_Timestamp: // 0x0304; // EXT_FUN_DAT sets A to tx hash of the first tx after $addr
												// timestamp
			return "A_To_Tx_After_Timestamp";
		case OpCode.Get_Type_For_Tx_In_A: // 0x0305; // EXT_FUN_RET if A is a valid tx then @addr to tx type*
			return "Get_Type_For_Tx_In_A";
		case OpCode.Get_Amount_For_Tx_In_A: // 0x0306; // EXT_FUN_RET if A is a valid tx then @addr to tx amount**
			return "Get_Amount_For_Tx_In_A";
		case OpCode.Get_Timestamp_For_Tx_In_A: // 0x0307; // EXT_FUN_RET if A is a valid tx then @addr to the tx
												// timestamp
			return "Get_Timestamp_For_Tx_In_A";
		case OpCode.Get_Random_Id_For_Tx_In_A: // 0x0308; // EXT_FUN_RET if A is a valid tx then @addr to the tx random
												// id***
			return "Get_Random_Id_For_Tx_In_A";
		case OpCode.Message_From_Tx_In_A_To_B: // 0x0309; // EXT_FUN if A is a valid tx then B to the tx message****
			return "Message_From_Tx_In_A_To_B";
		case OpCode.B_To_Address_Of_Tx_In_A: // 0x030a; // EXT_FUN if A is a valid tx then B set to the tx address
			return "B_To_Address_Of_Tx_In_A";
		case OpCode.B_To_Address_Of_Creator: // 0x030b; // EXT_FUN sets B to the address of the AT's creator
			return "B_To_Address_Of_Creator";

		case OpCode.Get_Current_Balance: // 0x0400; // EXT_FUN_RET sets @addr to current balance of the AT
			return "Get_Current_Balance";
		case OpCode.Get_Previous_Balance: // 0x0401; // EXT_FUN_RET sets @addr to the balance it had last had when
											// running*
			return "Get_Previous_Balance";
		case OpCode.Send_To_Address_In_B: // 0x0402; // EXT_FUN_DAT if B is a valid address then send it $addr amount**
			return "Send_To_Address_In_B";
		case OpCode.Send_All_To_Address_In_B: // 0x0403; // EXT_FUN if B is a valid address then send it the entire
												// balance
			return "Send_All_To_Address_In_B";
		case OpCode.Send_Old_To_Address_In_B: // 0x0404; // EXT_FUN if B is a valid address then send it the old
												// balance**
			return "Send_Old_To_Address_In_B";
		case OpCode.Send_A_To_Address_In_B: // 0x0405; // EXT_FUN if B is a valid address then send it A as a message
			return "Send_A_To_Address_In_B";
		case OpCode.Add_Minutes_To_Timestamp: // 0x0406; // EXT_FUN_RET_DAT_2 set @addr1 to timestamp $addr2 plus $addr3
												// minutes***
			return "Add_Minutes_To_Timestamp";

		default:
			return "UNKNOWN FUNCTION";
		}
	}
}
