package org.restcomm.connect.sms.smpp.dlr.provider;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class TelestaxDlrParserTest {

	@Test
	public void parseMessageTest(){
		final String pduMessage = "id:0000058049 sub:001 dlvrd:001 submit date:1805170144 done date:1805170144 stat:DELIVRD err:000 text:none";
		TelestaxDlrParser parser = new TelestaxDlrParser();
		Map<String,String> dlrMap = parser.parseMessage(pduMessage);
		System.out.println("dlrMap: "+dlrMap);
		Assert.assertEquals("0000058049", dlrMap.get("id"));
		Assert.assertEquals("DELIVRD", dlrMap.get("status"));
		Assert.assertEquals("1805170144", dlrMap.get("sent_date"));
		Assert.assertEquals("1805170144", dlrMap.get("submit_date"));
	}
}
