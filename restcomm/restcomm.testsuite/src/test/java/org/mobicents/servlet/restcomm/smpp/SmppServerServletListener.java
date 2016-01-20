package org.mobicents.servlet.restcomm.smpp;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.sip.SipFactory;

import org.apache.log4j.Logger;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppProcessingException;


public class SmppServerServletListener implements ServletContextListener{



	private final static Logger logger = Logger.getLogger(SmppServerServletListener.class);
	
	//Message to be sent from SMPP Server to Restcomm
	private final static String smppTo = "123456789";
	private final static String  smppFrom = "987654321";
	private final static String smppMessage = "Message from SMPP Server to Restcomm";
    private static ServletContext context;
    private SipFactory factory;



		@Override
		public void contextDestroyed(ServletContextEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void contextInitialized(ServletContextEvent event) {

            //context = event.getServletContext();
            
	        logger.info("************SERVLET LISTENER STARTED ******************");

	        
	        ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
	        ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(1, new ThreadFactory() {
	            private AtomicInteger sequence = new AtomicInteger(0);
	            @Override
	            public Thread newThread(Runnable r) {
	                Thread t = new Thread(r);
	                t.setName("SmppServerSessionWindowMonitorPool-" + sequence.getAndIncrement());
	                return t;
	            }
	        }); 
	        
	        // create a server configuration
	        SmppServerConfiguration configuration = new SmppServerConfiguration();
	        configuration.setPort(2776);
	        configuration.setMaxConnectionSize(10);
	        configuration.setNonBlockingSocketsEnabled(true);
	        configuration.setDefaultRequestExpiryTimeout(30000);
	        configuration.setDefaultWindowMonitorInterval(15000);
	        configuration.setDefaultWindowSize(5);
	        configuration.setDefaultWindowWaitTimeout(configuration.getDefaultRequestExpiryTimeout());
	        configuration.setDefaultSessionCountersEnabled(true);
	        configuration.setJmxEnabled(true);
	        
	        // create a server, start it up
	        DefaultSmppServer smppServer = new DefaultSmppServer(configuration, new DefaultSmppServerHandler(), executor, monitorExecutor);

			// TODO Auto-generated method stub
	        
	        try {
		        logger.info("Starting SMPP server...");
				smppServer.start();
		        logger.info("SMPP server started");

			} catch (SmppChannelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        logger.info("Server counters: {} " +  smppServer.getCounters());

	        
		}

		

	
    public static class DefaultSmppServerHandler implements SmppServerHandler {
    	
    	private static SmppServerSession smppServerSession ;


        @Override
        public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException {
            // test name change of sessions
            // this name actually shows up as thread context....
            sessionConfiguration.setName("Application.SMPP." + sessionConfiguration.getSystemId());

            //throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL, null);
        }

        @Override
        public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
            logger.info("Session created: {} " +  session);
            // need to do something it now (flag we're ready)
            session.serverReady(new TestSmppSessionHandler(session));

            smppServerSession = session;
            
            if (smppServerSession.isClosed()){
                logger.info("********Session Closed*******");
            }else if (smppServerSession.isOpen()){
                logger.info("********Session Open*******");
            }else if (smppServerSession.isBinding()){
                logger.info("********Session Binding*******");
                smppServerSession = session ;
            }else if (smppServerSession.isBound()){
                logger.info("********Session Bound*******");
                smppServerSession = session ;
            }

            
        }
        
        public static  SmppServerSession getSmppServerSession(){
   			return smppServerSession;
        	
        }

        @Override
        public void sessionDestroyed(Long sessionId, SmppServerSession session) {
            logger.info("Session destroyed: {} " + session);
            // print out final stats
            if (session.hasCounters()) {
                logger.info(" final session rx-submitSM: {} " + session.getCounters().getRxSubmitSM());
            }
            
            // make sure it's really shutdown
            session.destroy();
        }
    }

    public static class TestSmppSessionHandler extends DefaultSmppSessionHandler {
        
    	private static boolean smppOutBoundMessageReceivedByServer ;
    	
        private WeakReference<SmppSession> sessionRef;
        
        public TestSmppSessionHandler(SmppSession session) {
            this.sessionRef = new WeakReference<SmppSession>(session);
        }
        
        @Override
        public PduResponse firePduRequestReceived(PduRequest pduRequest) {
            SmppSession session = sessionRef.get();
            
            // mimic how long processing could take on a slower smsc
            //processing received SMPP message from Restcomm
            String decodedPduMessage = null;
            String destSmppAddress= null;
            String sourceSmppAddress= null;

                if( pduRequest.toString().toLowerCase().contains("enquire_link") ){
                     //logger.info("This is a response to the enquire_link, therefore, do NOTHING ");
                }else{

            		//smppOutBoundMessageReceivedByServer = true;
                    logger.info("********Restcomm Message Received By SMPP Server*******");

                    try {
                    SubmitSm deliverSm = (SubmitSm) pduRequest;
                    decodedPduMessage = CharsetUtil.CHARSET_MODIFIED_UTF8.decode(deliverSm.getShortMessage());
                    destSmppAddress = deliverSm.getDestAddress().getAddress();
                    sourceSmppAddress = deliverSm.getSourceAddress().getAddress();
                    //send received SMPP PDU message to restcomm
                    }
                    catch (Exception e) {                   
                    	logger.info("********DeliverSm Exception******* " + e);
                    	}

                    	if (destSmppAddress.equalsIgnoreCase("9898989") &&
                    			decodedPduMessage.equalsIgnoreCase("Test message") &&
                    			sourceSmppAddress.equalsIgnoreCase("alice") ){
                    		//Message is correctly received from Restcomm
                    		smppOutBoundMessageReceivedByServer = true;
                    	}else {
                    		smppOutBoundMessageReceivedByServer = false;
                    	}

                }
   
            return pduRequest.createResponse();
        }
        
        public static  boolean getSmppOutBoundMessageReceivedByServer(){
   			return smppOutBoundMessageReceivedByServer;
        	
        }
        
    }
   
    public static class SmppPrepareInboundMessage {

    	//private final ActorSystem system = ActorSystem.create("SmppActorSystem");	
        //private final ActorSystem system = SmppInitConfigurationDetails.getSystem();
    	private final static ActorSystem system = ActorSystem.create("SmppActorSystem");	
        private final static Logger logger = Logger.getLogger(SmppPrepareInboundMessage.class);
        
        //****************************************************************
        //
        //Use to send Inbound Message to Restcomm from Smpp Server
        //
        //****************************************************************
        
        public static  void sendMessageToRestcommFromSmppServer() throws IOException{
            logger.info("********INSIDE sendMessageToRestcommFromSmppServer ******* " );
        	sendSmppMessageToRestcomm(smppMessage, smppTo, smppFrom);
        }
        
        public static void sendSmppMessageToRestcomm (String smppMessage, String smppTo, String smppFrom) throws IOException{

            String to = smppTo;
            String from = smppFrom;
            String inboundMessage = smppMessage;
            logger.info("********to ******* " +to );
            logger.info("********From ******* " + from );
            logger.info("********inboundMessage ******* " + inboundMessage );
            
            SmppInboundMessageEntity smppInboundMessage =  new SmppInboundMessageEntity(to, from, inboundMessage );
            ActorRef sendIncomingSmppToSmppHandler = smppInboundToHandler();
            sendIncomingSmppToSmppHandler.tell(smppInboundMessage, null);

        }

        private static ActorRef smppInboundToHandler() {
            logger.info("********INSIDE THE THING******* " );
            return system.actorOf(new Props(new UntypedActorFactory() {
                private static final long serialVersionUID = 1L;

                @Override
                public UntypedActor create() throws Exception {
                    return new  SmppHandlerProcessMessages();
                }
            }));
        }
    	
    }

}
