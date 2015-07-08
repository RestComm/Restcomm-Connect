package org.mobicents.servlet.restcomm.email.api;

/**
 * Created by lefty on 6/26/15.
 */
public class EmailRequest {
    private final Mail emailmsg;

    public  EmailRequest(Mail object){
        super();
        this.emailmsg=object;
    }

    public Mail getObject(){
        return this.emailmsg;
    }

}
