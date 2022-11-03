package bt.dapps;

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

    //temporariy variables
    long registerFee = 5000000000000;
    long checkValue;
    long ValidatorToken;
    long decimalPlaces = 4;
    long temp1, temp2, temp3, temp4,temp5;

    public void registerChain(){
        if(getValue(INDEX_OWNER_KEY1,this.getId()) == ZERO){
            //Set Owner of the chain
            setMapValue(INDEX_OWNER_KEY1,this.getId(),this.getCurrentTxSender());
            //Set first validator for chain = owner
            setMapValue(this.getCurrentTxSender(),this.getId(),ONE);
            //Set minimum proofs
            setMapValue(INDEX_NUMBER_OF_PROOFS_KEY1,this.getId(),THREE);
            //SET Withdrawal Fee
            setMapValue(INDEX_WITHDRAWAL_FEE_KEY1,this.getId(),TEN);
            //Set Fee-Amouint collected 
            setMapValue(INDEX_WITHDRAWAL_FEE_QUANTITY_KEY1,this.getId(),ZERO);
        }
    }
    public void registerChainToken(long genesisBlockID, long wrappedTokenID, long ValidatorTokenName, long TokenDecimalFactor){
        if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getId()){
            // check if token is not set yet
            if(getValue(INDEX_TOKEN_ID_TO_BRIDGE_KEY1,genesisBlockID) == ZERO){
                // check if fee is paid
                if(this.getCurrentTxAmount() >= registerFee){
                    //Mint Token for validators and set token 
                    ValidatorToken = issueAsset(ValidatorTokenName, 0L, decimalPlaces);
                    setMapValue(INDEX_DAO_TOKEN_KEY1,genesisBlockID,ValidatorToken.getID());
                    setMapValue(INDEX_TOKEN_ID_TO_BRIDGE_KEY1,genesisBlockID,wrappedTokenID);
                    setMapValue(INDEX_TOKEN_DECIMALS_TO_BRIDGE_KEY1,genesisBlockID,TokenDecimalFactor);
                    // TokenDecimalFactor = 1 , 10 .... depending on decimals of the token 0 - 8
                    //sending RegisterFee-MintingFees to contract creator and also dust
                    sendAmount(getCurrentBalance(), getContractCreator());
                    continue;
                }
            }

        }
        //if not able to register, pay back the amount
        sendAmount(this.getCurrentTxAmount(),this.getCurrentTxSender())
    }
    public void withdrawWrappedToken(long sisterChainTxId, long accountIdSisterChain, long SisterChainAmount, long genesisBlockID ){
        //check if sender is validator and wrappedToken defined 
        if (getValue(this.getCurrentTxSender().getID(),genesisBlockID) == ONE && getMapValue(INDEX_TOKEN_ID_TO_BRIDGE_KEY1,genesisBlockID) != ZERO){
            //check if Validator already voted
            if(getValue(sisterChainTxId,this.getCurrentTxSender().getID()) == ZERO ){
                // check if vote was already initiated
                if (getValue(genesisBlockID,sisterChainTxId) == ZERO){
                    setValue(genesisBlockID,sisterChainTxId,this.getCurrentTX.getID());
                    checkValue = this.getCurrentTX.getID();
                }
                else{
                    checkValue = getValue(genesisBlockID,sisterChainTxId)
                }
                //Adding the vote  per amount and account
                temp1 = getValue(checkValue,SisterChainAmount) + ONE;
                setValue(checkValue,SisterChainAmount,temp1)
                temp2 = getValue(checkValue,accountIdSisterChain) + ONE;
                setValue(checkValue,accountIdSisterChain,temp2)
                //Set voted for validator - so he can´t do it again
                setValue(sisterChainTxId,this.getCurrentTxSender().getID(), ONE)
                //Check if vote reached min proof
                if(temp1 >= getMapValue(INDEX_NUMBER_OF_PROOFS_KEY1,genesisBlockID) && temp2 >= getMapValue(INDEX_NUMBER_OF_PROOFS_KEY1,genesisBlockID)){
                    //min proof is done
                    temp1 = getMapValue(INDEX_WITHDRAWAL_FEE_KEY1,genesisBlockID);
                    temp2 = getMapValue(INDEX_TOKEN_DECIMALS_TO_BRIDGE_KEY1,genesisBlockID);
                    temp3 = (SisterChainAmount * temp2 * temp1)/THOUSAND;
                    temp1 = SisterChainAmount * temp2- temp3;
                    //Add fees to index
                    temp4 = getMapValue(INDEX_WITHDRAWAL_FEE_QUANTITY_KEY1,genesisBlockID) + temp3;
                    setValue(INDEX_WITHDRAWAL_FEE_QUANTITY_KEY1,genesisBlockID,temp4);
                    //pay amount to account if balance covers it
                    temp5 = getMapValue(INDEX_TOKEN_ID_TO_BRIDGE_KEY1,genesisBlockID)
                    if (getCurrentBalance(temp5)> temp1){
                        sendAmount(getMapValue(INDEX_TOKEN_ID_TO_BRIDGE_KEY1,genesisBlockID),temp1,accountIdSisterChain);
                    }
                    //done - fee handling later tbd
                }
            }

        }
    }
    public void changeOwner(long genesisBlockID, long newOwner){
        if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getID()){
            setMapValue(INDEX_OWNER_KEY1,this.getId(),newOwner);
        }
    }
    public void changeMinProof(long genesisBlockID, long minimumProofs){
        if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getID()){
            if(minimumProofs > THREE){
                setMapValue(INDEX_NUMBER_OF_PROOFS_KEY1,genesisBlockID,minimumProofs);
            }
        }
    }
    public void changeWithdrawalFee(long genesisBlockID, long fee){
        if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getID() && fee >= ZERO && fee <= MAXFEE){
           setMapValue(INDEX_NUMBER_OF_PROOFS_KEY1,genesisBlockID,fee)
        }
   }

    public void addValidator(long genesisBlockID, long Validator){
         if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getID()){
            setMapValue(Validator,genesisBlockID,ONE)
         }
    }
    public void removeValidator(long genesisBlockID, long Validator){
        if (getValue(INDEX_OWNER_KEY1,genesisBlockID) == this.getCurrentTxSender().getID()){
           setMapValue(Validator,genesisBlockID,ZERO)
        }
   }

	private void saveValue(long key1, long key2, long value) {
		setMapValue(key1, key2, value);
	}
	private long getValue(long key1, long key2) {
		return getMapValue(key1, key2);
	}
}
