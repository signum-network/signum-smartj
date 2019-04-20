/*
 * Copyright (c) 2014 CIYAM Developers

 Distributed under the MIT/X11 software license, please refer to the file license.txt
 in the root project directory or http://www.opensource.org/licenses/mit-license.php.
*/

package bt.compiler;	

final class OpCode {
  static final byte e_op_code_NOP     = 0x7f;
  static final byte e_op_code_SET_VAL = 0x01;
  static final byte e_op_code_SET_DAT = 0x02;
  static final byte e_op_code_CLR_DAT = 0x03;
  static final byte e_op_code_INC_DAT = 0x04;
  static final byte e_op_code_DEC_DAT = 0x05;
  static final byte e_op_code_ADD_DAT = 0x06;
  static final byte e_op_code_SUB_DAT = 0x07;
  static final byte e_op_code_MUL_DAT = 0x08;
  static final byte e_op_code_DIV_DAT = 0x09;
  static final byte e_op_code_BOR_DAT = 0x0a;
  static final byte e_op_code_AND_DAT = 0x0b;
  static final byte e_op_code_XOR_DAT = 0x0c;
  static final byte e_op_code_NOT_DAT = 0x0d;
  static final byte e_op_code_SET_IND = 0x0e;
  static final byte e_op_code_SET_IDX = 0x0f;
  static final byte e_op_code_PSH_DAT = 0x10;
  static final byte e_op_code_POP_DAT = 0x11;
  static final byte e_op_code_JMP_SUB = 0x12;
  static final byte e_op_code_RET_SUB = 0x13;
  static final byte e_op_code_IND_DAT = 0x14;
  static final byte e_op_code_IDX_DAT = 0x15;
  static final byte e_op_code_MOD_DAT = 0x16;
  static final byte e_op_code_SHL_DAT = 0x17;
  static final byte e_op_code_SHR_DAT = 0x18;
  static final byte e_op_code_JMP_ADR = 0x1a;
  static final byte e_op_code_BZR_DAT = 0x1b;
  static final byte e_op_code_BNZ_DAT = 0x1e;
  static final byte e_op_code_BGT_DAT = 0x1f;
  static final byte e_op_code_BLT_DAT = 0x20;
  static final byte e_op_code_BGE_DAT = 0x21;
  static final byte e_op_code_BLE_DAT = 0x22;
  static final byte e_op_code_BEQ_DAT = 0x23;
  static final byte e_op_code_BNE_DAT = 0x24;
  static final byte e_op_code_SLP_DAT = 0x25;
  static final byte e_op_code_FIZ_DAT = 0x26;
  static final byte e_op_code_STZ_DAT = 0x27;
  static final byte e_op_code_FIN_IMD = 0x28;
  static final byte e_op_code_STP_IMD = 0x29;
  static final byte e_op_code_SLP_IMD = 0x2a;
  static final byte e_op_code_ERR_ADR = 0x2b;
  static final byte e_op_code_SET_PCS = 0x30;
  static final byte e_op_code_EXT_FUN = 0x32;
  static final byte e_op_code_EXT_FUN_DAT   = 0x33;
  static final byte e_op_code_EXT_FUN_DAT_2 = 0x34;
  static final byte e_op_code_EXT_FUN_RET   = 0x35;
  static final byte e_op_code_EXT_FUN_RET_DAT   = 0x36;
  static final byte e_op_code_EXT_FUN_RET_DAT_2 = 0x37;
  
  static final short Set_A1    = 0x0110; // EXT_FUN_DAT       sets A1 from $addr
  static final short Set_A2    = 0x0111; // EXT_FUN_DAT       sets A2 from $addr
  static final short Set_A3    = 0x0112; // EXT_FUN_DAT       sets A3 from $addr
  static final short Set_A4    = 0x0113; // EXT_FUN_DAT       sets A4 from $addr
  static final short Set_A1_A2 = 0x0114; // EXT_FUN_DAT_2     sets A1 from $addr1 and A2 from $addr2
  static final short Set_A3_A4 = 0x0115; // EXT_FUN_DAT_2     sets A3 from $addr1 and A4 from $addr2
  static final short Set_B1    = 0x0116; // EXT_FUN_DAT       sets B1 from $addr
  static final short Set_B2    = 0x0117; // EXT_FUN_DAT       sets B2 from $addr
  static final short Set_B3    = 0x0118; // EXT_FUN_DAT       sets B3 from $addr
  static final short Set_B4    = 0x0119; // EXT_FUN_DAT       sets B4 from $addr
  static final short Set_B1_B2 = 0x011a; // EXT_FUN_DAT_2     sets B1 from $addr1 and B2 from $addr2
  static final short Set_B3_B4 = 0x011b; // EXT_FUN_DAT_2     sets B3 from $addr1 and B4 from $addr2
  
  static final short Clear_A          = 0x0120; //  EXT_FUN           sets A to zero (A being A1..4)
  static final short Clear_B          = 0x0121; //  EXT_FUN           sets B to zero (B being B1..4)
  static final short Clear_A_And_B    = 0x0122; //  EXT_FUN           sets both A and B to zero
  static final short Copy_A_From_B    = 0x0123; //  EXT_FUN           copies B into A
  static final short Copy_B_From_A    = 0x0124; //  EXT_FUN           copies A into B
  static final short Check_A_Is_Zero  = 0x0125; //  EXT_FUN_RET       @addr to 1 if A is zero or 0 if it is not (i.e. bool)
  static final short Check_B_Is_Zero  = 0x0126; //  EXT_FUN_RET       @addr to 1 if B is zero of 0 if it is not (i.e. bool)
  static final short Check_A_Equals_B = 0x0127; //  EXT_FUN_RET       @addr to bool if A is equal to B
  static final short Swap_A_and_B     = 0x0128; //  EXT_FUN           swap the values of A and B
  static final short OR_A_with_B      = 0x0129; //  EXT_FUN           sets A to A | B (bitwise OR)
  static final short OR_B_with_A      = 0x012a; //  EXT_FUN           sets B to B | A (bitwise OR)
  static final short AND_A_with_B     = 0x012b; //  EXT_FUN           sets A to A & B (bitwise AND)
  static final short AND_B_with_A     = 0x012c; //  EXT_FUN           sets B to B & A (bitwise AND)
  static final short XOR_A_with_B     = 0x012d; //  EXT_FUN           sets A to A ^ B (bitwise XOR)
  static final short XOR_B_with_A     = 0x012e; //  EXT_FUN           sets B to B ^ A (bitwise XOR)
  
  static final short Get_A1   = 0x0100; // EXT_FUN_RET       sets @addr to A1
  static final short Get_A2   = 0x0101; // EXT_FUN_RET       sets @addr to A2
  static final short Get_A3   = 0x0102; // EXT_FUN_RET       sets @addr to A3
  static final short Get_A4   = 0x0103; // EXT_FUN_RET       sets @addr to A4
  static final short Get_B1   = 0x0104; // EXT_FUN_RET       sets @addr to B1
  static final short Get_B2   = 0x0105; // EXT_FUN_RET       sets @addr to B2
  static final short Get_B3   = 0x0106; // EXT_FUN_RET       sets @addr to B3
  static final short Get_B4   = 0x0107; // EXT_FUN_RET       sets @addr to B4
  
  static final short Get_Block_Timestamp       = 0x0300; // EXT_FUN_RET       sets @addr to the timestamp of the current block
  static final short Get_Creation_Timestamp    = 0x0301; // EXT_FUN_RET       sets @addr to the timestamp of the AT creation block
  static final short Get_Last_Block_Timestamp  = 0x0302; // EXT_FUN_RET       sets @addr to the timestamp of the previous block
  static final short Put_Last_Block_Hash_In_A  = 0x0303; // EXT_FUN           puts the block hash of the previous block in A
  static final short A_To_Tx_After_Timestamp   = 0x0304; // EXT_FUN_DAT       sets A to tx hash of the first tx after $addr timestamp
  static final short Get_Type_For_Tx_In_A      = 0x0305; // EXT_FUN_RET       if A is a valid tx then @addr to tx type*
  static final short Get_Amount_For_Tx_In_A    = 0x0306; // EXT_FUN_RET       if A is a valid tx then @addr to tx amount**
  static final short Get_Timestamp_For_Tx_In_A = 0x0307; // EXT_FUN_RET       if A is a valid tx then @addr to the tx timestamp
  static final short Get_Random_Id_For_Tx_In_A = 0x0308; // EXT_FUN_RET       if A is a valid tx then @addr to the tx random id***
  static final short Message_From_Tx_In_A_To_B = 0x0309; // EXT_FUN           if A is a valid tx then B to the tx message****
  static final short B_To_Address_Of_Tx_In_A   = 0x030a; // EXT_FUN           if A is a valid tx then B set to the tx address
  static final short B_To_Address_Of_Creator   = 0x030b; // EXT_FUN           sets B to the address of the AT's creator
  
  static final short Get_Current_Balance      = 0x0400; // EXT_FUN_RET       sets @addr to current balance of the AT
  static final short Get_Previous_Balance     = 0x0401; // EXT_FUN_RET       sets @addr to the balance it had last had when running*
  static final short Send_To_Address_In_B     = 0x0402; // EXT_FUN_DAT       if B is a valid address then send it $addr amount**
  static final short Send_All_To_Address_In_B = 0x0403; // EXT_FUN           if B is a valid address then send it the entire balance
  static final short Send_Old_To_Address_In_B = 0x0404; // EXT_FUN           if B is a valid address then send it the old balance**
  static final short Send_A_To_Address_In_B   = 0x0405; // EXT_FUN           if B is a valid address then send it A as a message
  static final short Add_Minutes_To_Timestamp = 0x0406; // EXT_FUN_RET_DAT_2 set @addr1 to timestamp $addr2 plus $addr3 minutes***

}
