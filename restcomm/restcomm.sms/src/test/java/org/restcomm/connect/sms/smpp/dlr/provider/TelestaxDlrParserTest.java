package org.restcomm.connect.sms.smpp.dlr.provider;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.restcomm.connect.dao.entities.SmsMessage;
import org.restcomm.connect.sms.smpp.dlr.spi.DlrParser;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;

public class TelestaxDlrParserTest {

	@Test
	public void parseDLRMessageTest(){
		final String pduMessage = "id:0000058049 sub:001 dlvrd:001 submit date:1805170144 done date:1805170144 stat:DELIVRD err:000 text:none                ";
		DlrParser parser = new TelestaxDlrParser();
		Map<String,String> dlrMap = parser.parseMessage(pduMessage);
		Assert.assertEquals("0000058049", dlrMap.get("id"));
		Assert.assertEquals("DELIVRD", dlrMap.get("status"));
		Assert.assertEquals("1805170144", dlrMap.get("sent_date"));
		Assert.assertEquals("1805170144", dlrMap.get("submit_date"));
	}
	
	@Test
	public void getRestcommStatus() throws SmppInvalidArgumentException {
		DlrParser parser = new TelestaxDlrParser();
		statusMap.put("ACCEPTD", SmsMessage.Status.QUEUED);
        statusMap.put("EXPIRED", SmsMessage.Status.FAILED);
        statusMap.put("DELETED", SmsMessage.Status.FAILED);
        statusMap.put("UNDELIV", SmsMessage.Status.FAILED);
        statusMap.put("REJECTD", SmsMessage.Status.FAILED);
        statusMap.put("UNKNOWN", SmsMessage.Status.SENDING);
		Assert.assertEquals(SmsMessage.Status.DELIVERED, parser.getRestcommStatus("DELIVRD"));
		Assert.assertEquals(SmsMessage.Status.DELIVERED, parser.getRestcommStatus("DELIVRD"));
		Assert.assertEquals(SmsMessage.Status.DELIVERED, parser.getRestcommStatus("DELIVRD"));
		Assert.assertEquals(SmsMessage.Status.DELIVERED, parser.getRestcommStatus("DELIVRD"));
		Assert.assertEquals(SmsMessage.Status.DELIVERED, parser.getRestcommStatus("DELIVRD"));
		Assert.assertEquals(SmsMessage.Status.DELIVERED, parser.getRestcommStatus("DELIVRD"));
		Assert.assertEquals(SmsMessage.Status.DELIVERED, parser.getRestcommStatus("DELIVRD"));
	}
}
