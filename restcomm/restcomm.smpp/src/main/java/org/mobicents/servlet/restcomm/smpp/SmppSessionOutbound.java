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
        String sourceAddress;
        String destinationAddress;

        final SipURI getUri = (SipURI) request.getRequestURI();
        String to = getUri.getUser();
        String smppDestPrefix = "smpp";
        //if there is no dest address after the prefix use address map in restcomm.xml
        if ( to.equalsIgnoreCase("smpp")){
            sourceAddress =  SmppService.getSmppSourceAddressMap();
            destinationAddress = SmppService.getSmppDestinationAddressMap() ;
        }else{
            sourceAddress =  SmppService.getSmppSourceAddressMap(); //get source from restcomm.xml file
          //remove the smpp prefix from the destination address
            destinationAddress = to.toLowerCase().substring(smppDestPrefix.length());
        }


        //make sure SMPP session is bound before attempting to send message
        if (smppSession.isBound() && smppSession != null){
            String requestMessage =  request.getContent().toString(); // get SMS from the SipServletRequest
            byte[] textBytes = CharsetUtil.encode(requestMessage, CharsetUtil.CHARSET_GSM);
            int smppTonNpiValue =  Integer.parseInt(SmppService.getSmppTonNpiValue()) ;
          /**
            String sourceAddress =  SmppService.getSmppSourceAddressMap();
            String destinationAddress = to.toLowerCase().substring(smppDestPrefix.length());  //SmppService.getSmppDestinationAddressMap() ;

**/

            // add delivery receipt
            //submit0.setRegisteredDelivery(SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED);
            SubmitSm submit0 = new SubmitSm();
            submit0.setSourceAddress(new Address((byte)smppTonNpiValue, (byte) smppTonNpiValue, sourceAddress ));
            submit0.setDestAddress(new Address((byte)smppTonNpiValue, (byte)smppTonNpiValue, destinationAddress));
            submit0.setShortMessage(textBytes);

       //send message to SMPP endpoint
                try {
                    SubmitSmResp submitResp = smppSession.submit(submit0, 10000);
                    //send response back to Restcomm
                  //  final SipServletResponse messageAccepted = request.createResponse(SipServletResponse.SC_ACCEPTED);
                  //  messageAccepted.send();

                } catch (RecoverablePduException | UnrecoverablePduException
                        | SmppTimeoutException | SmppChannelException
                        | InterruptedException e) {
                    // TODO Auto-generated catch block
                  //  logger.error("response after sending submit submitResp : " + submitResp );
                    e.printStackTrace();
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
        // TODO Auto-generated method stub

    }

}
