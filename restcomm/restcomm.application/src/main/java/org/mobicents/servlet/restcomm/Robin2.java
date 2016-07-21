package org.mobicents.servlet.restcomm;

//TODO: REMOVE THIS CLASS AFTER TESTING
//THIS IS A TEST CLASS, for some quick local tests, MUST BE IGNORED
public class Robin2 {

    private final int numberOfMediaGateways;
    private int round;
    private static Robin2 robin=null;

    //Singleton
    private Robin2(final int numberOfMediaGateways){
        this.numberOfMediaGateways = numberOfMediaGateways;
        round = 0;
    }

    public static Robin2 getInstance(final int numberOfMediaGateways){
        if(robin == null){
            robin = new Robin2(numberOfMediaGateways);
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
