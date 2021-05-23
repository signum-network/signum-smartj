package bt;

public abstract class SimpleNFTContract extends Contract {
    public Address owner;

    public SimpleNFTContract(){
        this.owner = this.getCreator();
    }

    public void transfer(Address receiver) {
        if(this.getCurrentTxSender() == this.owner){
            this.owner = receiver;
        }
    }

}
