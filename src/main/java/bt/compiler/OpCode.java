/*
 * Copyright (c) 2014 CIYAM Developers

 Distributed under the MIT/X11 software license, please refer to the file license.txt
 in the root project directory or http://www.opensource.org/licenses/mit-license.php.
*/

package bt.compiler;	

final class OpCode {
  static final byte e_op_code_NOP     = 0x7f; // Unused
  static final byte e_op_code_SET_VAL = 0x01;
  static final byte e_op_code_SET_DAT = 0x02;
  static final byte e_op_code_CLR_DAT = 0x03;
  static final byte e_op_code_INC_DAT = 0x04;
  static final byte e_op_code_DEC_DAT = 0x05; // Unused
  static final byte e_op_code_ADD_DAT = 0x06;
  static final byte e_op_code_SUB_DAT = 0x07;
  static final byte e_op_code_MUL_DAT = 0x08;
  static final byte e_op_code_DIV_DAT = 0x09;
  static final byte e_op_code_BOR_DAT = 0x0a;
  static final byte e_op_code_AND_DAT = 0x0b;
  static final byte e_op_code_XOR_DAT = 0x0c;
  static final byte e_op_code_NOT_DAT = 0x0d; // Unused
  static final byte e_op_code_SET_IND = 0x0e;
  static final byte e_op_code_SET_IDX = 0x0f; // Unused
  static final byte e_op_code_PSH_DAT = 0x10;
  static final byte e_op_code_POP_DAT = 0x11;
  static final byte e_op_code_JMP_SUB = 0x12;
  static final byte e_op_code_RET_SUB = 0x13;
  static final byte e_op_code_IND_DAT = 0x14;
  static final byte e_op_code_IDX_DAT = 0x15; // Unused
  static final byte e_op_code_MOD_DAT = 0x16;
  static final byte e_op_code_SHL_DAT = 0x17; // Unused
  static final byte e_op_code_SHR_DAT = 0x18; // Unused
  static final byte e_op_code_POW_DAT = 0x19;
  static final byte e_op_code_JMP_ADR = 0x1a;
  static final byte e_op_code_BZR_DAT = 0x1b;
  static final byte e_op_code_BNZ_DAT = 0x1e;
  static final byte e_op_code_BGT_DAT = 0x1f;
  static final byte e_op_code_BLT_DAT = 0x20;
  static final byte e_op_code_BGE_DAT = 0x21;
  static final byte e_op_code_BLE_DAT = 0x22;
  static final byte e_op_code_BEQ_DAT = 0x23; // Unused
  static final byte e_op_code_BNE_DAT = 0x24; // Unused
  static final byte e_op_code_SLP_DAT = 0x25;
  static final byte e_op_code_FIZ_DAT = 0x26; // Unused
  static final byte e_op_code_STZ_DAT = 0x27; // Unused
  static final byte e_op_code_FIN_IMD = 0x28; // Unused
  static final byte e_op_code_STP_IMD = 0x29; // Unused
  static final byte e_op_code_SLP_IMD = 0x2a;
  static final byte e_op_code_ERR_ADR = 0x2b; // Unused
  static final byte e_op_code_MDV_DAT = 0x2c;
  static final byte e_op_code_SET_PCS = 0x30;
  static final byte e_op_code_EXT_FUN = 0x32;
  static final byte e_op_code_EXT_FUN_DAT   = 0x33;
  static final byte e_op_code_EXT_FUN_DAT_2 = 0x34; // Unused
  static final byte e_op_code_EXT_FUN_RET   = 0x35;
  static final byte e_op_code_EXT_FUN_RET_DAT   = 0x36; // Unused
  static final byte e_op_code_EXT_FUN_RET_DAT_2 = 0x37;
  
  static final short Set_A1    = 0x0110; // EXT_FUN_DAT       sets A1 from $addr
  static final short Set_A2    = 0x0111; // EXT_FUN_DAT       sets A2 from $addr
  static final short Set_A3    = 0x0112; // EXT_FUN_DAT       sets A3 from $addr
  static final short Set_A4    = 0x0113; // EXT_FUN_DAT       sets A4 from $addr
  static final short Set_A1_A2 = 0x0114; // EXT_FUN_DAT_2     sets A1 from $addr1 and A2 from $addr2
  static final short Set_A3_A4 = 0x0115; // EXT_FUN_DAT_2     sets A3 from $addr1 and A4 from $addr2 // Unused
  static final short Set_B1    = 0x0116; // EXT_FUN_DAT       sets B1 from $addr
  static final short Set_B2    = 0x0117; // EXT_FUN_DAT       sets B2 from $addr // Unused
  static final short Set_B3    = 0x0118; // EXT_FUN_DAT       sets B3 from $addr // Unused
  static final short Set_B4    = 0x0119; // EXT_FUN_DAT       sets B4 from $addr // Unused
  static final short Set_B1_B2 = 0x011a; // EXT_FUN_DAT_2     sets B1 from $addr1 and B2 from $addr2
  static final short Set_B3_B4 = 0x011b; // EXT_FUN_DAT_2     sets B3 from $addr1 and B4 from $addr2 // Unused
  
  static final short Clear_A          = 0x0120; //  EXT_FUN           sets A to zero (A being A1..4)
  static final short Clear_B          = 0x0121; //  EXT_FUN           sets B to zero (B being B1..4) // Unused
  static final short Clear_A_And_B    = 0x0122; //  EXT_FUN           sets both A and B to zero // Unused
  static final short Copy_A_From_B    = 0x0123; //  EXT_FUN           copies B into A // Unused
  static final short Copy_B_From_A    = 0x0124; //  EXT_FUN           copies A into B // Unused
  static final short Check_A_Is_Zero  = 0x0125; //  EXT_FUN_RET       @addr to 1 if A is zero or 0 if it is not (i.e. bool) // Unused
  static final short Check_B_Is_Zero  = 0x0126; //  EXT_FUN_RET       @addr to 1 if B is zero of 0 if it is not (i.e. bool) // Unused
  static final short Check_A_Equals_B = 0x0127; //  EXT_FUN_RET       @addr to bool if A is equal to B // Unused
  static final short Swap_A_and_B     = 0x0128; //  EXT_FUN           swap the values of A and B // Unused
  static final short OR_A_with_B      = 0x0129; //  EXT_FUN           sets A to A | B (bitwise OR) // Unused
  static final short OR_B_with_A      = 0x012a; //  EXT_FUN           sets B to B | A (bitwise OR) // Unused
  static final short AND_A_with_B     = 0x012b; //  EXT_FUN           sets A to A & B (bitwise AND) // Unused
  static final short AND_B_with_A     = 0x012c; //  EXT_FUN           sets B to B & A (bitwise AND) // Unused
  static final short XOR_A_with_B     = 0x012d; //  EXT_FUN           sets A to A ^ B (bitwise XOR) // Unused
  static final short XOR_B_with_A     = 0x012e; //  EXT_FUN           sets B to B ^ A (bitwise XOR) // Unused
  
  static final short Get_A1   = 0x0100; // EXT_FUN_RET       sets @addr to A1
  static final short Get_A2   = 0x0101; // EXT_FUN_RET       sets @addr to A2 // Unused
  static final short Get_A3   = 0x0102; // EXT_FUN_RET       sets @addr to A3 // Unused
  static final short Get_A4   = 0x0103; // EXT_FUN_RET       sets @addr to A4 // Unused
  static final short Get_B1   = 0x0104; // EXT_FUN_RET       sets @addr to B1
  static final short Get_B2   = 0x0105; // EXT_FUN_RET       sets @addr to B2 // Unused
  static final short Get_B3   = 0x0106; // EXT_FUN_RET       sets @addr to B3 // Unused
  static final short Get_B4   = 0x0107; // EXT_FUN_RET       sets @addr to B4 // Unused

  static final short MD5_A_To_B               = 0x0200; //  EXT_FUN           take an MD5 hash of A1..2 and put this is B1..2 // Unused
  static final short Check_MD5_A_With_B       = 0x0201; //  EXT_FUN_RET       @addr to bool if MD5 hash of A1..2 matches B1..2 // Unused
  static final short HASH160_A_To_B           = 0x0202; //  EXT_FUN           take a RIPEMD160 hash of A1..3 and put this in B1..3 // Unused
  static final short Check_HASH160_A_With_B   = 0x0203; //  EXT_FUN_RET       @addr to bool if RIPEMD160 hash of A1..3 matches B1..3 // Unused
  static final short SHA256_A_To_B            = 0x0204; //  EXT_FUN           take a SHA256 hash of A and put this in B
  static final short Check_SHA256_A_With_B    = 0x0205; //  EXT_FUN_RET       @addr to bool if SHA256 hash of A matches B // Unused
  static final short CHECK_SIG_B_WITH_A       = 0x0206; //  EXT_FUN_RET       @addr to bool if [AT ID, B2..4] signature can be verified with the message attached on tx id in A1 (page in A2) for account id in A3

  
  static final short Get_Block_Timestamp       = 0x0300; // EXT_FUN_RET       sets @addr to the timestamp of the current block
  static final short Get_Creation_Timestamp    = 0x0301; // EXT_FUN_RET       sets @addr to the timestamp of the AT creation block
  static final short Get_Last_Block_Timestamp  = 0x0302; // EXT_FUN_RET       sets @addr to the timestamp of the previous block
  static final short Put_Last_Block_Hash_In_A  = 0x0303; // EXT_FUN           puts the block hash of the previous block in A
  static final short A_To_Tx_After_Timestamp   = 0x0304; // EXT_FUN_DAT       sets A to tx hash of the first tx after $addr timestamp
  static final short Get_Type_For_Tx_In_A      = 0x0305; // EXT_FUN_RET       if A is a valid tx then @addr to tx type* // Unused
  static final short Get_Amount_For_Tx_In_A    = 0x0306; // EXT_FUN_RET       if A is a valid tx then @addr to tx amount**
  static final short Get_Timestamp_For_Tx_In_A = 0x0307; // EXT_FUN_RET       if A is a valid tx then @addr to the tx timestamp
  static final short Get_Random_Id_For_Tx_In_A = 0x0308; // EXT_FUN_RET       if A is a valid tx then @addr to the tx random id*** // Unused
  static final short Message_From_Tx_In_A_To_B = 0x0309; // EXT_FUN           if A is a valid tx then B to the tx message****
  static final short B_To_Address_Of_Tx_In_A   = 0x030a; // EXT_FUN           if A is a valid tx then B set to the tx address
  static final short B_To_Address_Of_Creator   = 0x030b; // EXT_FUN           sets B to the address of the AT's creator
  static final short GET_CODE_HASH_ID          = 0x030c; // EXT_FUN_RET       sets @addr to the code hash ID of the AT's (or of the AT id on B2 if B2!=0)
  
  static final short Get_Current_Balance      = 0x0400; // EXT_FUN_RET       sets @addr to current balance of the AT
  static final short Get_Previous_Balance     = 0x0401; // EXT_FUN_RET       sets @addr to the balance it had last had when running* // Unused
  static final short Send_To_Address_In_B     = 0x0402; // EXT_FUN_DAT       if B is a valid address then send it $addr amount**
  static final short Send_All_To_Address_In_B = 0x0403; // EXT_FUN           if B is a valid address then send it the entire balance
  static final short Send_Old_To_Address_In_B = 0x0404; // EXT_FUN           if B is a valid address then send it the old balance** // Unused
  static final short Send_A_To_Address_In_B   = 0x0405; // EXT_FUN           if B is a valid address then send it A as a message
  static final short Add_Minutes_To_Timestamp = 0x0406; // EXT_FUN_RET_DAT_2 set @addr1 to timestamp $addr2 plus $addr3 minutes***
  
  static final short GET_MAP_VALUE_KEYS_IN_A  = 0x0407; // EXT_FUN_RET       keys in A1, A2, and A3 (if A3==0 use the AT ID as key3)
  static final short SET_MAP_VALUE_KEYS_IN_A  = 0x0408; // EXT_FUN           keys in A1 and A2 with value in A4
  static final short ISSUE_ASSET              = 0x0409; // EXT_FUN_RET       issue asset with name in A and dec. places in B1, returns the asset id
  static final short MINT_ASSET               = 0x040a; // EXT_FUN           mint B1 quantity of asset ID in B2
  static final short DIST_TO_ASSET_HOLDERS    = 0x040b; // EXT_FUN           B1 min holding of asset ID in B2, A1 the signa amount to distribute, A3 the assetId to distribute, A4 the asset quantity to distribute
  static final short GET_ASSET_HOLDERS_COUNT  = 0x040c; // EXT_FUN_RET       number of tokens holders with more than B1 holdings of asset ID in B2
  static final short GET_ACTIVATION_FEE       = 0x040d; // EXT_FUN_RET       sets @addr to the activation fee of this AT (or the AT id on B2 if B2!=0)
  static final short PUT_LAST_BLOCK_GSIG_IN_A = 0x040e; // EXT_FUN           puts the block generation signature of the previous block in A

}
