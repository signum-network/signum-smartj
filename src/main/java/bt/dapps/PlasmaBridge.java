package bt.dapps;

import bt.Address;
import bt.Contract;
import bt.Register;
import bt.Timestamp;
import bt.Transaction;


public class PlasmaBridge extends Contract {
    // Index defintions
    public static final long INDEX_OWNER_KEY1 = 1;
    public static final long INDEX_NUMBER_OF_PROOFS_KEY1 = 2;
    public static final long INDEX_WITHDRAWAL_FEE_KEY1 =3;
    public static final long INDEX_DAO_TOKEN_KEY1 = 4;
    public static final long INDEX_WITHDRAWAL_FEE_QUANTITY_KEY1 = 5;
    public static final long INDEX_TOKEN_ID_TO_BRIDGE_KEY1 = 6;
    public static final long INDEX_TOKEN_DECIMALS_TO_BRIDGE_KEY1 = 7;
    public static final long ZERO = 0;
    public static final long ONE = 1;
    public static final long THREE = 3;
    public static final long TEN = 10;
    public static final long MAXFEE = 250;
    public static final long THOUSAND = 1000;
	public static final long CONTRACT_FEES = 120000000;
    //temporariy variables
    long registerFee = 5000000000000L;
    long checkValue;
    long ValidatorToken;
    long decimalPlaces = 4;
    long temp1, temp2, temp3, temp4,temp5;
    long TokenDecimalFactor;

    public void registerChain(){
        if(getValue(INDEX_OWNER_KEY1,this.getCurrentTx().getId()) == ZERO){
            //Set Owner of the chain
            saveValue(INDEX_OWNER_KEY1,this.getCurrentTx().getId(),this.getCurrentTxSender().getId());
            //Set first validator for chain = owner
            saveValue(this.getCurrentTxSender().getId(),this.getCurrentTx().getId(),ONE);
            //Set minimum proofs
            saveValue(INDEX_NUMBER_OF_PROOFS_KEY1,this.getCurrentTx().getId(),THREE);
            //SET Withdrawal Fee
            saveValue(INDEX_WITHDRAWAL_FEE_KEY1,this.getCurrentTx().getId(),TEN);
            //Set Fee-Amouint collected 
            saveValue(INDEX_WITHDRAWAL_FEE_QUANTITY_KEY1,this.getCurrentTx().getId(),ZERO);
        }
    }
    public void registerChainToken(long genesisBlockID, long wrappedTokenID, long ValidatorTokenName){
        TokenDecimalFactor = getCurrentTx().getMessage(ONE).getValue1();
        if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getId()){
            // check if token is not set yet
            if(getValue(INDEX_TOKEN_ID_TO_BRIDGE_KEY1,genesisBlockID) == ZERO){
                // check if fee is paid
                if(this.getCurrentTxAmount() >= registerFee){
                    //Mint Token for validators and set token 
                    ValidatorToken = issueAsset(ValidatorTokenName, 0L, decimalPlaces);
                    saveValue(INDEX_DAO_TOKEN_KEY1,genesisBlockID,ValidatorToken);
                    saveValue(INDEX_TOKEN_ID_TO_BRIDGE_KEY1,genesisBlockID,wrappedTokenID);
                    saveValue(INDEX_TOKEN_DECIMALS_TO_BRIDGE_KEY1,genesisBlockID,TokenDecimalFactor);
                    // TokenDecimalFactor = 1 , 10 .... depending on decimals of the token 0 - 8
                    //sending RegisterFee-MintingFees to contract creator and also dust
                    sendAmount(getCurrentBalance()-CONTRACT_FEES, getCreator());
                }
            }

        }
        //if not able to register, pay back the amount
        sendAmount(this.getCurrentTxAmount(),this.getCurrentTxSender());
    }
    public void withdrawWrappedToken(long sisterChainTxId, Address accountIdSisterChain, long SisterChainAmount, long genesisBlockID ){
        //check if sender is validator and wrappedToken defined 
        if (getValue(this.getCurrentTxSender().getId(),genesisBlockID) == ONE && getMapValue(INDEX_TOKEN_ID_TO_BRIDGE_KEY1,genesisBlockID) != ZERO){
            //check if Validator already voted
            if(getValue(sisterChainTxId,this.getCurrentTxSender().getId()) == ZERO ){
                // check if vote was already initiated
                if (getValue(genesisBlockID,sisterChainTxId) == ZERO){
                    saveValue(genesisBlockID,sisterChainTxId,this.getCurrentTx().getId());
                    checkValue = this.getCurrentTx().getId();
                }
                else{
                    checkValue = getValue(genesisBlockID,sisterChainTxId);
                }
                //Adding the vote  per amount and account
                temp1 = getValue(checkValue,SisterChainAmount) + ONE;
                saveValue(checkValue,SisterChainAmount,temp1);
                temp2 = getValue(checkValue,accountIdSisterChain.getId()) + ONE;
                saveValue(checkValue,accountIdSisterChain.getId(),temp2);
                //Set voted for validator - so he can´t do it again
                saveValue(sisterChainTxId,this.getCurrentTxSender().getId(), ONE);
                //Check if vote reached min proof
                if(temp1 >= getMapValue(INDEX_NUMBER_OF_PROOFS_KEY1,genesisBlockID) && temp2 >= getMapValue(INDEX_NUMBER_OF_PROOFS_KEY1,genesisBlockID)){
                    //min proof is done
                    temp1 = getMapValue(INDEX_WITHDRAWAL_FEE_KEY1,genesisBlockID);
                    temp2 = getMapValue(INDEX_TOKEN_DECIMALS_TO_BRIDGE_KEY1,genesisBlockID);
                    temp3 = (SisterChainAmount * temp2 * temp1)/THOUSAND;
                    temp1 = SisterChainAmount * temp2- temp3;
                    //Add fees to index
                    temp4 = getMapValue(INDEX_WITHDRAWAL_FEE_QUANTITY_KEY1,genesisBlockID) + temp3;
                    saveValue(INDEX_WITHDRAWAL_FEE_QUANTITY_KEY1,genesisBlockID,temp4);
                    //pay amount to account if balance covers it
                    temp5 = getMapValue(INDEX_TOKEN_ID_TO_BRIDGE_KEY1,genesisBlockID);
                    if (getCurrentBalance(temp5)> temp1){
                        sendAmount(getMapValue(INDEX_TOKEN_ID_TO_BRIDGE_KEY1,genesisBlockID),temp1,accountIdSisterChain);
                    }
                    //done - fee handling later tbd
                }
            }

        }
    }
    public void changeContractOwner(long genesisBlockID, long newOwner){
        if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getId()){
            setMapValue(INDEX_OWNER_KEY1,genesisBlockID,newOwner);
        }
    }
    public void changeMinProof(long genesisBlockID, long minimumProofs){
        if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getId()){
            if(minimumProofs > THREE){
                setMapValue(INDEX_NUMBER_OF_PROOFS_KEY1,genesisBlockID,minimumProofs);
            }
        }
    }
    public void changeWithdrawalFee(long genesisBlockID, long fee){
        if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getId() && fee >= ZERO && fee <= MAXFEE){
           setMapValue(INDEX_NUMBER_OF_PROOFS_KEY1,genesisBlockID,fee);
        }
   }

    public void addValidator(long genesisBlockID, long Validator){
         if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getId()){
            setMapValue(Validator,genesisBlockID,ONE);
         }
    }
    public void removeValidator(long genesisBlockID, long Validator){
        if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getId()){
           setMapValue(Validator,genesisBlockID,ZERO);
        }
   }

	private void saveValue(long key1, long key2, long value) {
		setMapValue(key1, key2, value);
	}
	private long getValue(long key1, long key2) {
		return getMapValue(key1, key2);
	}
    public void txReceived() {
        // do nothing, since we are using a loop on blockStarted over all transactions
      }
}
