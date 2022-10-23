package bt.compiler;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Helper class to print bytecode as assembly code.
 * This is actually a machine code decompiler.
 * 
 * @author jjos
 */
public class Printer {

	private final static char[] hexArray = "0123456789abcdef".toCharArray();

    static short readShort(byte[] code, int needle) {
        ByteBuffer address = ByteBuffer.allocate(2);
        address.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 2; i++) {
            address.put(code[needle + i]);
        }
        address.clear();
        return address.getShort();
    }

    static int readInt(byte[] code, int needle) {
        ByteBuffer address = ByteBuffer.allocate(4);
        address.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 4; i++) {
            address.put(code[needle + i]);
        }
        address.clear();
        return address.getInt();
    }

    /** Note: Prints content in backwards to simulate swap endianess */
    static int printHex(byte[] bytes, int start, int length, PrintStream out) {
		for (int j = length - 1; j >= 0; j--) {
            printByte(bytes[j + start], out);
		}
		return length;
	}

    static void printByte(byte v, PrintStream out) {
        int c1 = (v & 0xF0) >> 4;
        int c2 = v & 0x0F;
        out.print(hexArray[c1]);
        out.print(hexArray[c2]);
    }

    static String getVariableName(int address, Compiler c ) {
        if (address == c.lastTxReceived)
            return "_lastTxReceived";
        if (address == c.lastTxTimestamp)
            return "_lastTxTimestamp";
        if (address == c.lastTxSender)
            return "_lastTxSender";
        if (address == c.lastTxAmount)
            return "_lastTxAmount";
        if (address == c.creator)
            return "_creator";
        if (address == c.tmpVar1)
            return "r0";
        if (address == c.tmpVar2)
            return "r1";
        if (address == c.tmpVar3)
            return "r2";
        if (address == c.tmpVar4)
            return "r3";
        if (address == c.tmpVar5)
            return "r4";
        if (address == c.tmpVar6)
            return "r5";
        if (address == c.localStart)
            return "_localStart";
        // Look into fields
        for (Field fi : c.fields.values()) {
            if (fi.address == address) {
                return fi.node.name;
            }
        }
        // Return unknow variable
        return "_var" + address;
    }

	static int printVariableAddress(byte[] code, int p, PrintStream out, Compiler c) {
		if (c != null) {
            out.print(getVariableName(readInt(code, p), c));
		}
		return 4;
	}

    static int printLabelAddress(byte[] code, int p, PrintStream out) {
        out.print(addressToLabel(readInt(code, p)));
		return 4;
	}

    static String methodNameToLabel(String fname) {
        if (fname.equals("<init>")) {
            return "_constructor";
        }
        return "_method_" + fname;
    }

	static int printMethodAddress(byte[] code, int p, PrintStream out, Compiler c) {
		if (c != null) {
			int ad = readInt(code, p);

			// print the method name if found
			for (Method mi : c.getMethods()) {
				if (mi.address == ad) {
                    out.print(methodNameToLabel(mi.node.name));
					break;
				}
			}
		}
		return 4;
	}

	static int printLabelForThisOpCode(byte[] bytes, int start, int length, PrintStream out) {
        out.println(addressToLabel(start) + ":");
		return length;
	}

	public static void printCode(byte[] code, PrintStream out) {
        int p = 0;
		while (p < code.length) {
			printByte(code[p++], out);
		}

	}

	public static void print(byte[] code, PrintStream out, Compiler c) {
		print(code, code.length, out, c);
	}

    public static String addressToLabel(int address) {
        String hexStr = Integer.toHexString(address).toUpperCase();
        return "_L" + "0000".substring(hexStr.length()) + hexStr;
    }

	public static void print(byte[] code, int length, PrintStream out, Compiler c) {

		int p = 0;
        String fname;
        int opCodeStartAddress;

        // ^declare
        for (int x=0; x < c.getDataPages() * 32; x++) {
            out.println("^declare " + getVariableName(x, c));
        }
        out.println();

		while (p < length) {
			if (p > 0 && c!=null) {
				// check if this is the start position of a method
				for (Method m : c.getMethods()) {
					if (m.address == p) {
                        out.println();
                        out.println(methodNameToLabel(m.getName()) + ":");
                    }
				}
			}

			byte op = code[p];
            opCodeStartAddress = p;
            out.println(addressToLabel(opCodeStartAddress) + ":");
            p++;
			switch (op) {
			case OpCode.e_op_code_NOP:
				out.println("\tNOP");
				break;
			case OpCode.e_op_code_SET_VAL:
				out.print("\tSET @");
				p += printVariableAddress(code, p, out, c);
				out.print(" #");
				p += printHex(code, p, 8, out);
                out.println();
				break;

			case OpCode.e_op_code_SET_DAT:
				out.print("\tSET @");
				p += printVariableAddress(code, p, out, c);
                out.print(" $");
				p += printVariableAddress(code, p, out, c);
                out.println();
				break;
			case OpCode.e_op_code_CLR_DAT:
			case OpCode.e_op_code_INC_DAT:
			case OpCode.e_op_code_DEC_DAT:
			case OpCode.e_op_code_NOT_DAT:
				switch (op) {
				case OpCode.e_op_code_CLR_DAT:
					out.print("\tCLR @");
					break;
				case OpCode.e_op_code_INC_DAT:
					out.print("\tINC @");
					break;
				case OpCode.e_op_code_DEC_DAT:
					out.print("\tDEC @");
					break;
				case OpCode.e_op_code_NOT_DAT:
					out.print("\tNOT @");
					break;
				}
				p += printVariableAddress(code, p, out, c);
                out.println();
				break;
			case OpCode.e_op_code_PSH_DAT:
                out.print("\tPSH $");
                p += printVariableAddress(code, p, out, c);
                out.println();
                break;
            case OpCode.e_op_code_POP_DAT:
				out.print("\tPOP @");
				p += printVariableAddress(code, p, out, c);
                out.println();
				break;
			case OpCode.e_op_code_JMP_SUB:
				out.print("\tJSR :");
				p += printMethodAddress(code, p, out, c);
                out.println();
				break;
			case OpCode.e_op_code_JMP_ADR:
				out.print("\tJMP :");
                p += printLabelAddress(code, p, out);
                out.println();
				break;
			case OpCode.e_op_code_ADD_DAT:
			case OpCode.e_op_code_SUB_DAT:
			case OpCode.e_op_code_MUL_DAT:
			case OpCode.e_op_code_DIV_DAT:
			case OpCode.e_op_code_BOR_DAT:
			case OpCode.e_op_code_AND_DAT:
			case OpCode.e_op_code_XOR_DAT:
			case OpCode.e_op_code_MOD_DAT:
            case OpCode.e_op_code_POW_DAT:
				switch (op) {
				case OpCode.e_op_code_ADD_DAT:
					out.print("\tADD @");
					break;
				case OpCode.e_op_code_SUB_DAT:
					out.print("\tSUB @");
					break;
				case OpCode.e_op_code_MUL_DAT:
					out.print("\tMUL @");
					break;
				case OpCode.e_op_code_DIV_DAT:
					out.print("\tDIV @");
					break;
				case OpCode.e_op_code_BOR_DAT:
					out.print("\tBOR @");
					break;
				case OpCode.e_op_code_AND_DAT:
					out.print("\tAND @");
					break;
				case OpCode.e_op_code_XOR_DAT:
					out.print("\tXOR @");
					break;
				case OpCode.e_op_code_MOD_DAT:
					out.print("\tMOD @");
					break;
                case OpCode.e_op_code_POW_DAT:
					out.print("\tPOW @");
					break;
				}
				p += printVariableAddress(code, p, out, c);
                out.print(" $");
				p += printVariableAddress(code, p, out, c);
                out.println();
				break;
			case OpCode.e_op_code_SET_IND:
                out.print("\tSET @");
                p += printVariableAddress(code, p, out, c);
                out.print(" $($");
                p += printVariableAddress(code, p, out, c);
                out.println(")");
                break;
            case OpCode.e_op_code_IND_DAT:
				out.print("\tSET @($");
				p += printVariableAddress(code, p, out, c);
                out.print(") $");
				p += printVariableAddress(code, p, out, c);
                out.println();
				break;

			case OpCode.e_op_code_SET_IDX:
                out.print("\tSET @");
                p += printVariableAddress(code, p, out, c);
                out.print(" $($");
                p += printVariableAddress(code, p, out, c);
                out.print(" + $");
                p += printVariableAddress(code, p, out, c);
                out.println(")");
            case OpCode.e_op_code_IDX_DAT:
				out.print("\tSET @($");
				p += printVariableAddress(code, p, out, c);
                out.print(" + $");
				p += printVariableAddress(code, p, out, c);
                out.print(") $");
				p += printVariableAddress(code, p, out, c);
                out.println();
				break;

            case OpCode.e_op_code_MDV_DAT:
				out.print("\tMDV @");
				p += printVariableAddress(code, p, out, c);
                out.print(" $");
				p += printVariableAddress(code, p, out, c);
                out.print(" $");
				p += printVariableAddress(code, p, out, c);
                out.println();
				break;
			case OpCode.e_op_code_RET_SUB:
				out.println("\tRET");
				break;

			case OpCode.e_op_code_SET_PCS:
				out.println("\tPCS");
				break;
			case OpCode.e_op_code_EXT_FUN:
			case OpCode.e_op_code_EXT_FUN_DAT:
            case OpCode.e_op_code_EXT_FUN_DAT_2:
				out.print("\tFUN ");
				out.print(getApiName(readShort(code, p)));
                p += 2;
                if (op == OpCode.e_op_code_EXT_FUN_DAT) {
                    out.print(" $");
                    p += printVariableAddress(code, p, out, c);
                } else if (op == OpCode.e_op_code_EXT_FUN_DAT_2) {
                    out.print(" $");
                    p += printVariableAddress(code, p, out, c);
                    out.print(" $");
                    p += printVariableAddress(code, p, out, c);
                }
				out.println();
				break;
            case OpCode.e_op_code_EXT_FUN_RET:
            case OpCode.e_op_code_EXT_FUN_RET_DAT:
			case OpCode.e_op_code_EXT_FUN_RET_DAT_2:
                // Assembly exception: first argument printed before API name
				out.print("\tFUN @");
                fname = getApiName(readShort(code, p));
                p += 2;
				p += printVariableAddress(code, p, out, c);
                out.print(" " + fname);
                if (op == OpCode.e_op_code_EXT_FUN_RET_DAT) {
                    out.print(" $");
                    p += printVariableAddress(code, p, out, c);
                } else if (op == OpCode.e_op_code_EXT_FUN_RET_DAT_2) {
                    out.print(" $");
                    p += printVariableAddress(code, p, out, c);
                    out.print(" $");
                    p += printVariableAddress(code, p, out, c);
                }
                out.println();
                break;
			case OpCode.e_op_code_BZR_DAT:
			case OpCode.e_op_code_BNZ_DAT:
				out.print(op == OpCode.e_op_code_BZR_DAT ? "\tBZR $" : "\tBNZ $");
				p += printVariableAddress(code, p, out, c);
                out.println(" :" + addressToLabel(opCodeStartAddress + code[p]));
                p++;
				break;

			case OpCode.e_op_code_BGT_DAT:
			case OpCode.e_op_code_BLT_DAT:
			case OpCode.e_op_code_BGE_DAT:
			case OpCode.e_op_code_BLE_DAT:
			case OpCode.e_op_code_BEQ_DAT:
			case OpCode.e_op_code_BNE_DAT:
				switch (op) {
				case OpCode.e_op_code_BGT_DAT:
					out.print("\tBGT $");
					break;
				case OpCode.e_op_code_BLT_DAT:
					out.print("\tBLT $");
					break;
				case OpCode.e_op_code_BGE_DAT:
					out.print("\tBGE $");
					break;
				case OpCode.e_op_code_BLE_DAT:
					out.print("\tBLE $");
					break;
				case OpCode.e_op_code_BEQ_DAT:
					out.print("\tBEQ $");
					break;
				case OpCode.e_op_code_BNE_DAT:
					out.print("\tBNE $");
					break;
				}
				p += printVariableAddress(code, p, out, c);
                out.print(" $");
				p += printVariableAddress(code, p, out, c);
                out.println(" :" + addressToLabel(opCodeStartAddress + code[p]));
                p++;
				break;

			case OpCode.e_op_code_SLP_DAT:
				out.print("\tSLP $");
				p += printVariableAddress(code, p, out, c);
                out.println();
				break;
			case OpCode.e_op_code_SLP_IMD:
				out.println("\tSLP");
				break;

			case OpCode.e_op_code_FIN_IMD:
				out.println("\tFIN");
				break;

			case OpCode.e_op_code_SHL_DAT:
            case OpCode.e_op_code_SHR_DAT:
                out.print(op == OpCode.e_op_code_SHL_DAT ? "\tSHL @" : "\tSHR @");
                p += printVariableAddress(code, p, out, c);
                out.print(" $");
                p += printVariableAddress(code, p, out, c);
                out.println();
                break;
			case OpCode.e_op_code_FIZ_DAT:
			case OpCode.e_op_code_STZ_DAT:
			case OpCode.e_op_code_STP_IMD:
			case OpCode.e_op_code_ERR_ADR:

			default:
				out.println("\tUNSUPPORTED OpCode 0x" +  Integer.toHexString(op));
				break;
			}
		}
	}

	static String getApiName(short v) {
		switch (v) {
		case OpCode.Set_A1: // 0x0110; // EXT_FUN_DAT sets A1 from $addr
			return "set_A1";
		case OpCode.Set_A2: // 0x0111; // EXT_FUN_DAT sets A2 from $addr
			return "set_A2";
		case OpCode.Set_A3: // 0x0112; // EXT_FUN_DAT sets A3 from $addr
			return "set_A3";
		case OpCode.Set_A4: // 0x0113; // EXT_FUN_DAT sets A4 from $addr
			return "set_A4";
		case OpCode.Set_A1_A2: // 0x0114; // EXT_FUN_DAT_2 sets A1 from $addr1 and A2 from $addr2
			return "set_A1_A2";
		case OpCode.Set_A3_A4: // 0x0115; // EXT_FUN_DAT_2 sets A3 from $addr1 and A4 from $addr2
			return "set_A3_A4";
		case OpCode.Set_B1: // 0x0116; // EXT_FUN_DAT sets B1 from $addr
			return "set_B1";
		case OpCode.Set_B2: // 0x0117; // EXT_FUN_DAT sets B2 from $addr
			return "set_B2";
		case OpCode.Set_B3: // 0x0118; // EXT_FUN_DAT sets B3 from $addr
			return "set_B3";
		case OpCode.Set_B4: // 0x0119; // EXT_FUN_DAT sets B4 from $addr
			return "set_B4";
		case OpCode.Set_B1_B2: // 0x011a; // EXT_FUN_DAT_2 sets B1 from $addr1 and B2 from $addr2
			return "set_B1_B2";
		case OpCode.Set_B3_B4: // 0x011b; // EXT_FUN_DAT_2 sets B3 from $addr1 and B4 from $addr2
			return "set_B3_B4";

		case OpCode.Clear_A: // 0x0120; // EXT_FUN sets A to zero (A being A1..4)
			return "clear_A";
		case OpCode.Clear_B: // 0x0121; // EXT_FUN sets B to zero (B being B1..4)
			return "clear_B";
		case OpCode.Clear_A_And_B: // 0x0122; // EXT_FUN sets both A and B to zero
			return "clear_A_B";
		case OpCode.Copy_A_From_B: // 0x0123; // EXT_FUN copies B into A
			return "copy_A_From_B";
		case OpCode.Copy_B_From_A: // 0x0124; // EXT_FUN copies A into B
			return "copy_B_From_A";
		case OpCode.Check_A_Is_Zero: // 0x0125; // EXT_FUN_RET @addr to 1 if A is zero or 0 if it is not (i.e. bool)
			return "check_A_Is_Zero";
		case OpCode.Check_B_Is_Zero: // 0x0126; // EXT_FUN_RET @addr to 1 if B is zero of 0 if it is not (i.e. bool)
			return "check_B_Is_Zero";
		case OpCode.Check_A_Equals_B: // 0x0127; // EXT_FUN_RET @addr to bool if A is equal to B
			return "check_A_equals_B";
		case OpCode.Swap_A_and_B: // 0x0128; // EXT_FUN swap the values of A and B
			return "swap_A_and_B";
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
			return "get_A1";
		case OpCode.Get_A2: // 0x0101; // EXT_FUN_RET sets @addr to A2
			return "get_A2";
		case OpCode.Get_A3: // 0x0102; // EXT_FUN_RET sets @addr to A3
			return "get_A3";
		case OpCode.Get_A4: // 0x0103; // EXT_FUN_RET sets @addr to A4
			return "get_A4";
		case OpCode.Get_B1: // 0x0104; // EXT_FUN_RET sets @addr to B1
			return "get_B1";
		case OpCode.Get_B2: // 0x0105; // EXT_FUN_RET sets @addr to B2
			return "get_B2";
		case OpCode.Get_B3: // 0x0106; // EXT_FUN_RET sets @addr to B3
			return "get_B3";
		case OpCode.Get_B4: // 0x0107; // EXT_FUN_RET sets @addr to B4
			return "get_B4";

		case OpCode.Get_Block_Timestamp: // 0x0300; // EXT_FUN_RET sets @addr to the timestamp of the current block
			return "get_Block_Timestamp";
		case OpCode.Get_Creation_Timestamp: // 0x0301; // EXT_FUN_RET sets @addr to the timestamp of the AT creation
											// block
			return "get_Creation_Timestamp";
		case OpCode.Get_Last_Block_Timestamp: // 0x0302; // EXT_FUN_RET sets @addr to the timestamp of the previous
												// block
			return "get_Last_Block_Timestamp";
		case OpCode.Put_Last_Block_Hash_In_A: // 0x0303; // EXT_FUN puts the block hash of the previous block in A
			return "put_Last_Block_Hash_In_A";
		case OpCode.A_To_Tx_After_Timestamp: // 0x0304; // EXT_FUN_DAT sets A to tx hash of the first tx after $addr
												// timestamp
			return "A_to_Tx_after_Timestamp";
		case OpCode.Get_Type_For_Tx_In_A: // 0x0305; // EXT_FUN_RET if A is a valid tx then @addr to tx type*
			return "get_Type_for_Tx_in_A";
		case OpCode.Get_Amount_For_Tx_In_A: // 0x0306; // EXT_FUN_RET if A is a valid tx then @addr to tx amount**
			return "get_Amount_for_Tx_in_A";
		case OpCode.Get_Timestamp_For_Tx_In_A: // 0x0307; // EXT_FUN_RET if A is a valid tx then @addr to the tx
												// timestamp
			return "get_Timestamp_for_Tx_in_A";
		case OpCode.Get_Random_Id_For_Tx_In_A: // 0x0308; // EXT_FUN_RET if A is a valid tx then @addr to the tx random
												// id***
			return "Get_Ticket_Id_for_Tx_in_A";
		case OpCode.Message_From_Tx_In_A_To_B: // 0x0309; // EXT_FUN if A is a valid tx then B to the tx message****
			return "message_from_Tx_in_A_to_B";
		case OpCode.B_To_Address_Of_Tx_In_A: // 0x030a; // EXT_FUN if A is a valid tx then B set to the tx address
			return "B_to_Address_of_Tx_in_A";
		case OpCode.B_To_Address_Of_Creator: // 0x030b; // EXT_FUN sets B to the address of the AT's creator
			return "B_to_Address_of_Creator";

		case OpCode.Get_Current_Balance: // 0x0400; // EXT_FUN_RET sets @addr to current balance of the AT
			return "get_Current_Balance";
		case OpCode.Get_Previous_Balance: // 0x0401; // EXT_FUN_RET sets @addr to the balance it had last had when
											// running*
			return "get_Previous_Balance";
		case OpCode.Send_To_Address_In_B: // 0x0402; // EXT_FUN_DAT if B is a valid address then send it $addr amount**
			return "send_to_Address_in_B";
		case OpCode.Send_All_To_Address_In_B: // 0x0403; // EXT_FUN if B is a valid address then send it the entire
												// balance
			return "send_All_to_Address_in_B";
		case OpCode.Send_Old_To_Address_In_B: // 0x0404; // EXT_FUN if B is a valid address then send it the old
												// balance**
			return "send_Old_to_Address_in_B";
		case OpCode.Send_A_To_Address_In_B: // 0x0405; // EXT_FUN if B is a valid address then send it A as a message
			return "send_A_to_Address_in_B";
		case OpCode.Add_Minutes_To_Timestamp: // 0x0406; // EXT_FUN_RET_DAT_2 set @addr1 to timestamp $addr2 plus $addr3
												// minutes***
			return "add_Minutes_to_Timestamp";

		case OpCode.MD5_A_To_B: // = 0x0200; // EXT_FUN take an MD5 hash of A1..2 and put this is B1..2
			return "MD5_A_to_B";
		case OpCode.Check_MD5_A_With_B: // = 0x0201; // EXT_FUN_RET @addr to bool if MD5 hash of A1..2 matches B1..2
			return "check_MD5_A_with_B";
		case OpCode.HASH160_A_To_B: // = 0x0202; // EXT_FUN take a RIPEMD160 hash of A1..3 and put this in B1..3
			return "HASH160_A_to_B";
		case OpCode.Check_HASH160_A_With_B: // = 0x0203; // EXT_FUN_RET @addr to bool if RIPEMD160 hash of A1..3 matches
										// B1..3
			return "check_HASH160_A_with_B";
		case OpCode.SHA256_A_To_B: // = 0x0204; // EXT_FUN take a SHA256 hash of A and put this in B
			return "SHA256_A_to_B";
		case OpCode.Check_SHA256_A_With_B: // = 0x0205; // EXT_FUN_RET @addr to bool if SHA256 hash of A matches B
			return "check_SHA256_A_with_B";

		case OpCode.CHECK_SIG_B_WITH_A:
			return "Check_Sig_B_With_A";
		case OpCode.GET_CODE_HASH_ID:
			return "Get_Code_Hash_Id";
			
		case OpCode.MINT_ASSET:
			return "Mint_Asset";
		case OpCode.ISSUE_ASSET:
			return "Issue_Asset";
		case OpCode.DIST_TO_ASSET_HOLDERS:
			return "Distribute_To_Asset_Holders";
		case OpCode.GET_ACTIVATION_FEE:
			return "Get_Activation_Fee";
        case OpCode.PUT_LAST_BLOCK_GSIG_IN_A:
            return "Put_Last_Block_GSig_In_A";

		case OpCode.GET_MAP_VALUE_KEYS_IN_A:
			return "Get_Map_Value_Keys_In_A";
		case OpCode.SET_MAP_VALUE_KEYS_IN_A:
			return "Set_Map_Value_Keys_In_A";

		default:
			return "UNKNOWN FUNCTION (0x" + Integer.toHexString(v) + ")";
		}
	}
}
