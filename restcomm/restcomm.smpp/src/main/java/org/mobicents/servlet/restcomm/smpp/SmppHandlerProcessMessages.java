package org.mobicents.servlet.restcomm.smpp;



import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SmppHandlerProcessMessages extends UntypedActor  {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private final ActorSystem system = SmppInitConfigurationDetails.getSystem();


    @Override
    public void onReceive(Object message) throws Exception {
        // TODO Auto-generated method stub
        if (message instanceof SmppInboundMessageEntity){
            logger.info("Inbound Message is received by the SMPP Handler");
            ActorRef forwardInboundSmppMessage = smppHandlerProcessInboundMessages();
            forwardInboundSmppMessage.tell( message, null);
        }else if(message instanceof SmppOutboundMessageEntity ){
            logger.info("Outbound Message is Received by the SMPP Handler ");
            ActorRef forwardOutboundSmppMessage = smppHandlerProcessOutboundMessages();
            forwardOutboundSmppMessage.tell(message, null);
        }

    }

    private ActorRef smppHandlerProcessInboundMessages() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;
            @Override
            public UntypedActor create() throws Exception {
                return new SmppHandlerInboundForwarder();
            }
        }));
    }

    private ActorRef smppHandlerProcessOutboundMessages() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;
            @Override
            public UntypedActor create() throws Exception {
                return new SmppHandlerOutboundForwarder();
            }
        }));
    }


    /*
    public SmppMessageEntity convertMessage(SipServletRequest message) throws  IOException{
        final SipURI sipUri = (SipURI) message.getTo().getURI() ;
        final SipURI fromUri =  (SipURI) message.getFrom().getURI();
        final String smppFrom = fromUri.getUser();
        final String smppTo = sipUri.getUser();
        final String smppContent = message.getContent().toString();
        final SmppMessageEntity sms = new SmppMessageEntity(smppTo, smppFrom, smppContent);
        return sms;
    }
     **/

}
