package org.mobicents.servlet.restcomm.smpp;

import java.io.IOException;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;

import akka.actor.UntypedActor;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;


public class SmppSessionOutbound extends UntypedActor {

    private static final Logger logger = Logger.getLogger(SmppSessionOutbound.class);

    public SmppSessionOutbound(){
        super();
    }


    public void sendSmsFromRestcommToSmpp(SipServletRequest request) throws SmppInvalidArgumentException,  IOException{

        //get SMPP session from
        SmppSession smppSession = SmppClientOpsThread.getSmppSessionForOutbound();
       final SipURI getUri = (SipURI) request.getRequestURI();
        String to = getUri.getUser();
        String getFrom = request.getFrom().getURI().toString()  ; // .getURI().toString();
        //extract source address from example sip:1234564@sip.nexmo.com
        int start = "sip:".length();
        String textEnd = "@";
        int end = getFrom.indexOf(textEnd);
        String from = getFrom.substring(start, end);

        String sourceAddress = from;
        String destinationAddress = to;


        //make sure SMPP session is bound before attempting to send message
        if (smppSession.isBound() && smppSession != null){
            String requestMessage =  request.getContent().toString(); // get SMS from the SipServletRequest
            byte[] textBytes = CharsetUtil.encode(requestMessage, CharsetUtil.CHARSET_GSM);
            int smppTonNpiValue =  Integer.parseInt(SmppService.getSmppTonNpiValue()) ;
            // add delivery receipt
            //submit0.setRegisteredDelivery(SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED);
            SubmitSm submit0 = new SubmitSm();
            submit0.setSourceAddress(new Address((byte)smppTonNpiValue, (byte) smppTonNpiValue, sourceAddress ));
            submit0.setDestAddress(new Address((byte)smppTonNpiValue, (byte)smppTonNpiValue, destinationAddress));
            submit0.setShortMessage(textBytes);
               try {
                    SubmitSmResp submitResp = smppSession.submit(submit0, 10000);

                } catch (RecoverablePduException | UnrecoverablePduException
                        | SmppTimeoutException | SmppChannelException
                        | InterruptedException e) {
                    // TODO Auto-generated catch block
                    logger.error("SMPP message cannot be sent : " + e );
                }
        }else{
            logger.error("Message cannot be sent because SMPP session is not yet bound");
        }

    }


    @Override
    public void onReceive(Object message) throws Exception {

        if ( message instanceof SipServletRequest){
            sendSmsFromRestcommToSmpp((SipServletRequest)message);
        }

    }

}
