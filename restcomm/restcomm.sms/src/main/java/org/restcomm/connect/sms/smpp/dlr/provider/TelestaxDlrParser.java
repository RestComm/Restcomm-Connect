package org.restcomm.connect.sms.smpp.dlr.provider;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.restcomm.connect.dao.entities.SmsMessage;
import org.restcomm.connect.dao.entities.SmsMessage.Status;
import org.restcomm.connect.sms.smpp.dlr.spi.DLRPayload;
import org.restcomm.connect.sms.smpp.dlr.spi.DlrParser;
import org.restcomm.connect.commons.dao.Error;

public class TelestaxDlrParser implements DlrParser {

    private static final Logger logger = Logger.getLogger(TelestaxDlrParser.class);
    private static final Map<String, SmsMessage.Status> statusMap;
    private static final Map<String, Error> errorMap;

    private static final String TAG_SEPARATOR = " ";
    private static final String TAG_VALUE_SEPARATOR = ":";
    private static final String ID_TAG = "id" + TAG_VALUE_SEPARATOR;
    private static final String SUBMIT_TAG = "submit date" + TAG_VALUE_SEPARATOR;
    private static final String DONE_TAG = "done date" + TAG_VALUE_SEPARATOR;
    private static final String STAT_TAG = "stat" + TAG_VALUE_SEPARATOR;
    private static final String ERR_TAG = "err" + TAG_VALUE_SEPARATOR;
    private static final String DLVRD_TAG = "dlvrd" + TAG_VALUE_SEPARATOR;
    private static final String SUB_TAG = "sub" + TAG_VALUE_SEPARATOR;

    private static final DateTimeFormatter DLR_SENT_FORMAT = DateTimeFormat.forPattern("yyMMddHHmm");

    private static final String SUCCESS_CODE = "000";

    static {
        statusMap = new HashMap<String, SmsMessage.Status>();
        statusMap.put("ACCEPTD", SmsMessage.Status.SENT);
        statusMap.put("UNDELIV", SmsMessage.Status.UNDELIVERED);
        statusMap.put("EXPIRED", SmsMessage.Status.FAILED);
        statusMap.put("DELETED", SmsMessage.Status.FAILED);
        statusMap.put("REJECTD", SmsMessage.Status.FAILED);
        statusMap.put("DELIVRD", SmsMessage.Status.DELIVERED);
        statusMap.put("UNKNOWN", SmsMessage.Status.SENT);

        errorMap = new HashMap<String, Error>();
        //TODO change according to SMSc spec
        errorMap.put("001", Error.UNKNOWN_DESTINATION_HANDSET);
        errorMap.put("002", Error.UNKNOWN_DESTINATION_HANDSET);
        errorMap.put("003", Error.UNKNOWN_DESTINATION_HANDSET);
        errorMap.put("004", Error.MESSAGE_BLOCKED);
        errorMap.put("005", Error.MESSAGE_BLOCKED);
        errorMap.put("007", Error.MESSAGE_BLOCKED);
        errorMap.put("008", Error.UNREACHABLE_DESTINATION_HANDSET);
        errorMap.put("010", Error.LANDLINE_OR_UNREACHABLE_CARRIER);
        errorMap.put("011", Error.UNREACHABLE_DESTINATION_HANDSET);
        errorMap.put("012", Error.UNKNOWN_ERROR);
        errorMap.put("013", Error.UNKNOWN_ERROR);
        errorMap.put("014", Error.UNKNOWN_ERROR);
        errorMap.put("022", Error.MESSAGE_BLOCKED);
        errorMap.put("023", Error.UNREACHABLE_DESTINATION_HANDSET);
        errorMap.put("034", Error.UNKNOWN_ERROR);
        errorMap.put("038", Error.UNKNOWN_DESTINATION_HANDSET);
        errorMap.put("039", Error.UNKNOWN_DESTINATION_HANDSET);
        errorMap.put("040", Error.UNKNOWN_ERROR);
        errorMap.put("045", Error.UNKNOWN_ERROR);
        errorMap.put("051", Error.UNKNOWN_ERROR);
        errorMap.put("194", Error.UNKNOWN_ERROR);
        errorMap.put("224", Error.MESSAGE_BLOCKED);
        errorMap.put("225", Error.MESSAGE_BLOCKED);
        errorMap.put("226", Error.UNKNOWN_ERROR);
        errorMap.put("227", Error.UNKNOWN_ERROR);
        errorMap.put("228", Error.UNREACHABLE_DESTINATION_HANDSET);
        errorMap.put("229", Error.MESSAGE_BLOCKED);
        errorMap.put("230", Error.MESSAGE_BLOCKED);
        errorMap.put("231", Error.CARRIER_VIOLATION);
        errorMap.put("232", Error.UNKNOWN_DESTINATION_HANDSET);

    }

    private String parseTagValue(String message, String tagName) {
        String tagValue = null;
        int startPos = message.indexOf(tagName);
        if (startPos >= 0) {
            int tagStartPos = startPos + tagName.length();
            final int endPos = message.indexOf(TAG_SEPARATOR, tagStartPos);
            if (endPos >= 0) {
                tagValue = message.substring(tagStartPos, endPos);
            }
        }
        return tagValue;
    }

    /**
     * Parses given string following this format
     * https://help.nexmo.com/hc/en-us/articles/204015663-What-is-Nexmo-s-SMPP-DLR-format-
     *
     * @param message
     * @return the parsed DLR
     */
    @Override
    public DLRPayload parseMessage(String message) {
        //"id:XXXXXXXXXX sub:001 dlvrd:000 submit date:YYMMDDHHMM done date:YYMMDDHHMM stat:ZZZZZZZ err:YYY text:none
        DLRPayload dlr = new DLRPayload();

        final String messageId = parseTagValue(message, ID_TAG);
        dlr.setId(messageId);

        final String submit_date = parseTagValue(message, SUBMIT_TAG);
        DateTime parsedDate = parseDate(submit_date);
        dlr.setSubmitDate(parsedDate);

        final String done_date = parseTagValue(message, DONE_TAG);
        DateTime doneDate = parseDate(done_date);
        dlr.setDoneDate(doneDate);

        final String stat = parseTagValue(message, STAT_TAG);
        Status parsedStatus = parseRestcommStatus(stat);
        dlr.setStat(parsedStatus);

        final String err = parseTagValue(message, ERR_TAG);
        Error parsedError = parseRestcommErrorCode(err);
        dlr.setErr(parsedError);

        final String sub = parseTagValue(message, SUB_TAG);
        dlr.setSub(sub);

        final String dlv = parseTagValue(message, DLVRD_TAG);
        dlr.setDlvrd(dlv);

        if (logger.isDebugEnabled()) {
            logger.debug("Parsed DLR is:" + dlr);
        }
        return dlr;
    }

    private DateTime parseDate(String message) {
        //FIXME: should we actually have this default?
        DateTime date = DateTime.now();
        try {
            DateTime.parse(message, DLR_SENT_FORMAT);
        } catch (Exception e) {
            logger.debug("failed to parse date", e);
        }
        return date;
    }

    private Status parseRestcommStatus(String message) {
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
        if (statusMap.containsKey(message)) {
            status = statusMap.get(message);
        } else {
            logger.debug("received an unexpected Status message " + message);
        }

        return status;
    }

    private Error parseRestcommErrorCode(String errCode) {
        Error error = null;
        if (SUCCESS_CODE.equals(errCode)) {
            //set to null so no error is shown
            error = null;
        } else if (errorMap.containsKey(errCode)) {
            error = errorMap.get(errCode);
        } else {
            //if error is not in mapping table, set it to unknown
            error = Error.UNKNOWN_ERROR;
            logger.debug("received an unexpected error message " + error);
        }
        return error;
    }

}
