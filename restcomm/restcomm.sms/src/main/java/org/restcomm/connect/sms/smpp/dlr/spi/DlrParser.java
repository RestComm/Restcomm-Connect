package org.restcomm.connect.sms.smpp.dlr.spi;

import java.util.Map;

import org.joda.time.DateTime;
import org.restcomm.connect.dao.entities.SmsMessage;

public interface DlrParser {
    Map<String, String> parseMessage(String message);
    DateTime getDate(String message);
    String getError(String message);
    SmsMessage.Status getRestcommStatus(String message);
}