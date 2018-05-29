package org.restcomm.connect.sms.smpp.dlr.provider;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.restcomm.connect.dao.entities.SmsMessage;
import org.restcomm.connect.dao.entities.SmsMessage.Status;
import org.restcomm.connect.sms.smpp.dlr.spi.DlrParser;

public class TelestaxDlrParser implements DlrParser {
    private static final Logger logger = Logger.getLogger(TelestaxDlrParser.class);
    private static final Map<String, SmsMessage.Status> statusMap;
    static
    {
        statusMap = new HashMap<String, SmsMessage.Status>();
        statusMap.put("ACCEPTD", SmsMessage.Status.SENT);
        statusMap.put("UNDELIV", SmsMessage.Status.UNDELIVERED);
        statusMap.put("EXPIRED", SmsMessage.Status.FAILED);
        statusMap.put("DELETED", SmsMessage.Status.FAILED);
        statusMap.put("REJECTD", SmsMessage.Status.FAILED);
        statusMap.put("DELIVRD", SmsMessage.Status.DELIVERED);
        statusMap.put("UNKNOWN", SmsMessage.Status.SENT);
    }

    @Override
    public Map<String, String> parseMessage(String message) {
        //"id:XXXXXXXXXX sub:001 dlvrd:000 submit date:YYMMDDHHMM done date:YYMMDDHHMM stat:ZZZZZZZ err:YYY text:none
        Map<String,String> dlr = new HashMap<String, String>();

        final String idTag = "id:";
        final String submitTag = "submit date:";
        final String doneTag = "done date:";
        final String statTag = "stat:";
        final int idStartPos = message.indexOf(idTag)+idTag.length();
        final int submitStartPos = message.indexOf(submitTag)+submitTag.length();
        final int doneStartPos = message.indexOf(doneTag)+doneTag.length();
        final int statStartPos = message.indexOf(statTag)+statTag.length();
        final int idEndPos = message.indexOf(" ",idStartPos);
        final int submitEndPos = message.indexOf(" ",submitStartPos);
        final int doneEndPos = message.indexOf(" ",doneStartPos);
        final int statEndPos = message.indexOf(" ",statStartPos);
        final String messageId  = message.substring(idStartPos, idEndPos);
        final String submit_date = message.substring(submitStartPos, submitEndPos);
        final String done_date = message.substring(doneStartPos, doneEndPos);
        final String stat = message.substring(statStartPos, statEndPos);

        dlr.put("id", messageId);
        dlr.put("submit_date", submit_date);
        dlr.put("sent_date", done_date);
        dlr.put("status", stat);

        return dlr;
    }

    @Override
    public DateTime getDate(String message) {
        //FIXME: should we actually have this default?
        DateTime date = DateTime.now();
        try {
            DateTime.parse(message, DateTimeFormat.forPattern("yyMMddHHmm"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    public String getError(String message) {
        //TODO: create error map
        return "";
    }

    @Override
    public Status getRestcommStatus(String message) {
        /*
        https://help.nexmo.com/hc/en-us/articles/204015663-What-is-Nexmo-s-SMPP-DLR-format-
        DELIVRD Message is delivered to destination
        EXPIRED Message validity period has expired
        DELETED Message has been deleted
        ACCEPTD Message is in accepted state
        UNDELIV Message is undeliverable
        UNKNOWN Message is in invalid state
        REJECTD Message is in a rejected state
        */
        Status status = null;
        if(statusMap.containsKey(message)) {
            status = statusMap.get(message);
        } else {
            logger.error("received an unexpected Status message "+message);
        }

        return status;
    }

}
