package org.restcomm.connect.testsuite.smpp;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;

import org.apache.log4j.Logger;

import com.cloudhopper.commons.charset.Charset;
import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppConstants;
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

import org.restcomm.connect.sms.smpp.SmppInboundMessageEntity;

public class MockSmppServer {

    private static final Logger logger = Logger.getLogger(MockSmppServer.class);

    private final DefaultSmppServer smppServer;
    private static SmppServerSession smppServerSession;
    private static boolean linkEstablished = false;
    private static boolean messageSent = false;
    private static SmppInboundMessageEntity smppInboundMessageEntity;
    private static boolean messageReceived;

    public MockSmppServer() throws SmppChannelException {
        this(2776);
    }

    public MockSmppServer(int port) throws SmppChannelException {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, new ThreadFactory() {
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
        configuration.setHost("127.0.0.1");
        configuration.setPort(port);
        configuration.setMaxConnectionSize(10);
        configuration.setNonBlockingSocketsEnabled(true);
        configuration.setDefaultRequestExpiryTimeout(30000);
        configuration.setDefaultWindowMonitorInterval(15000);
        configuration.setDefaultWindowSize(5);
        configuration.setDefaultWindowWaitTimeout(configuration.getDefaultRequestExpiryTimeout());
        configuration.setDefaultSessionCountersEnabled(true);
        configuration.setJmxEnabled(true);

        // create a server, start it up
        smppServer = new DefaultSmppServer(configuration, new DefaultSmppServerHandler(), executor, monitorExecutor);
        smppServer.start();
        logger.info("SMPP server started! Server counters: {} " + smppServer.getCounters());
    }

    public void sendSmppMessageToRestcomm(String smppMessage, String smppTo, String smppFrom, Charset charset) throws IOException, SmppInvalidArgumentException {
        //http://stackoverflow.com/a/25885741
        try {
            byte[] textBytes;
            textBytes = CharsetUtil.encode(smppMessage, charset);

            DeliverSm deliver = new DeliverSm();

            deliver.setSourceAddress(new Address((byte) 0x03, (byte) 0x00, smppFrom));
            deliver.setDestAddress(new Address((byte) 0x01, (byte) 0x01, smppTo));
            deliver.setShortMessage(textBytes);
            if (CharsetUtil.CHARSET_UCS_2 == charset) {
                deliver.setDataCoding(SmppConstants.DATA_CODING_UCS2);
            } else {
                deliver.setDataCoding(SmppConstants.DATA_CODING_DEFAULT);
            }
            logger.info("deliver.getDataCoding: " + deliver.getDataCoding());

            WindowFuture<Integer, PduRequest, PduResponse> future = smppServerSession.sendRequestPdu(deliver, 10000, false);
            if (!future.await()) {
                logger.error("Failed to receive deliver_sm_resp within specified time");
            } else if (future.isSuccess()) {
                DeliverSmResp deliverSmResp = (DeliverSmResp) future.getResponse();
                messageSent = true;
                logger.info("deliver_sm_resp: commandStatus [" + deliverSmResp.getCommandStatus() + "=" + deliverSmResp.getResultMessage() + "]");
            } else {
                logger.error("Failed to properly receive deliver_sm_resp: " + future.getCause());
            }
        } catch (Exception e) {
            logger.fatal("Exception during sending SMPP message to Restcomm: " + e);
        }
    }

    public void stop() {
        if (smppServerSession != null) {
            smppServerSession.close();
            smppServerSession.destroy();
        }
    }

    public void cleanup() {
        this.messageSent = false;
        this.messageReceived = false;
        this.smppInboundMessageEntity = null;
    }

    public static boolean isLinkEstablished() {
        return linkEstablished;
    }

    public static boolean isMessageSent() {
        return messageSent;
    }

    public static SmppInboundMessageEntity getSmppInboundMessageEntity() {
        return smppInboundMessageEntity;
    }

    public static boolean isMessageReceived() {
        return messageReceived;
    }

    public int getPort() {
        return smppServer.getConfiguration().getPort();
    }

    private static class DefaultSmppServerHandler implements SmppServerHandler {

        @Override
        public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException {
            // test name change of sessions
            // this name actually shows up as thread context....
            sessionConfiguration.setName("Application.SMPP." + sessionConfiguration.getSystemId());

            //throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL, null);
        }

        @Override
        public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
            logger.info("Session created: {} " + session);
            // need to do something it now (flag we're ready)
            session.serverReady(new TestSmppSessionHandler(session));
            smppServerSession = session;
            if (smppServerSession.isClosed()) {
                logger.info("********Session Closed*******");
            } else if (smppServerSession.isOpen()) {
                logger.info("********Session Open*******");
            } else if (smppServerSession.isBinding()) {
                logger.info("********Session Binding*******");
                smppServerSession = session;
            } else if (smppServerSession.isBound()) {
                logger.info("********Session Bound*******");
                smppServerSession = session;
                linkEstablished = true;
            }
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

    private static class TestSmppSessionHandler extends DefaultSmppSessionHandler {

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
            byte dcs = 0;
            String destSmppAddress = null;
            String sourceSmppAddress = null;
            boolean isDeliveryReceipt = false;
            //FIXME: make MockSmppServer configurable
            Charset charset = CharsetUtil.CHARSET_UTF_8;

            if (pduRequest.toString().toLowerCase().contains("enquire_link")) {
                //logger.info("This is a response to the enquire_link, therefore, do NOTHING ");
            } else {

                //smppOutBoundMessageReceivedByServer = true;
                logger.info("********Restcomm Message Received By SMPP Server*******");

                try {
                    SubmitSm deliverSm = (SubmitSm) pduRequest;

                    dcs = deliverSm.getDataCoding();
                    if(dcs==SmppConstants.DATA_CODING_UCS2) {
                        charset = CharsetUtil.CHARSET_UCS_2;
                    }

                    decodedPduMessage = CharsetUtil.decode(deliverSm.getShortMessage(), charset);
                    destSmppAddress = deliverSm.getDestAddress().getAddress();
                    sourceSmppAddress = deliverSm.getSourceAddress().getAddress();
                    if (deliverSm.getRegisteredDelivery() == (byte) 0x01) {
                        isDeliveryReceipt = true;
                    }
                    logger.info("getDataCoding: " + deliverSm.getDataCoding());
                    //send received SMPP PDU message to restcomm
                } catch (Exception e) {
                    logger.info("********DeliverSm Exception******* " + e);
                }


                smppInboundMessageEntity = new SmppInboundMessageEntity(destSmppAddress, sourceSmppAddress, decodedPduMessage, charset, isDeliveryReceipt);
                messageReceived = true;
            }
            return pduRequest.createResponse();
        }
    }
}
