package org.mobicents.servlet.restcomm;


public class Robin {

    private final int numberOfMediaGateways;
    private int round;
    private static Robin robin=null;

    //Singleton
    private Robin(final int numberOfMediaGateways){
        this.numberOfMediaGateways = numberOfMediaGateways;
        round = 0;
    }

    public static Robin getInstance(final int numberOfMediaGateways){
        if(robin == null){
            robin = new Robin(numberOfMediaGateways);
            return robin;
        }
        return robin;
    }

    public int getNextMediaGatewayIndex(){
        return getRound();
    }

    private int getRound(){
        if(round >= numberOfMediaGateways){
            this.round=1;
            return 0;
        }else{
            return this.round++;
        }
    }

}
