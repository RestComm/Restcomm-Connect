package org.restcomm.connect.sms.smpp.dlr.provider;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.restcomm.connect.dao.entities.SmsMessage;
import org.restcomm.connect.sms.smpp.dlr.spi.DlrParser;

/**
 * @author mariafarooq
 *
 */
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
	public void getRestcommStatusTest() {
		DlrParser parser = new TelestaxDlrParser();
		Assert.assertEquals(SmsMessage.Status.DELIVERED, parser.getRestcommStatus("DELIVRD"));
		Assert.assertEquals(SmsMessage.Status.SENT, parser.getRestcommStatus("ACCEPTD"));
		Assert.assertEquals(SmsMessage.Status.FAILED, parser.getRestcommStatus("EXPIRED"));
		Assert.assertEquals(SmsMessage.Status.FAILED, parser.getRestcommStatus("DELETED"));
		Assert.assertEquals(SmsMessage.Status.UNDELIVERED, parser.getRestcommStatus("UNDELIV"));
		Assert.assertEquals(SmsMessage.Status.FAILED, parser.getRestcommStatus("REJECTD"));
		Assert.assertEquals(SmsMessage.Status.SENT, parser.getRestcommStatus("UNKNOWN"));
	}
}
