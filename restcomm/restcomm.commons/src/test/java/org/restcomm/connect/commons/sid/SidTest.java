package org.restcomm.connect.commons.sid;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;

public class SidTest {

	/**
	 * testSidConversion String to Sid conversation
	 */
	@Test
	public void testSidConversion() {
		String sidString = "AC6dcfdfd531e44ae4ac30e8f97e071ab2";
		try{
			Sid sid = new Sid(sidString);
			assertTrue(sid != null);
		}catch(Exception e){
			fail("invalid validation");
		}
	}

	/**
	 * testNewCallSid: String to Sid conversation
	 */
	@Test
	public void testNewCallSid() {
		String sidString = "ID6dcfdfd531e44ae4ac30e8f97e071122-CA6dcfdfd531e44ae4ac30e8f97e071ab2";
		try{
			Sid sid = new Sid(sidString);
			assertTrue(sid != null);
		}catch(Exception e){
			fail("invalid validation");
		}
	}

	/**
	 * testOldCallSid: it should be supported
	 * because from call api we can check older cdrs where sid was in old format
	 * and sid conversion should work
	 */
	@Test
	public void testOldCallSid() {
		String sidString = "CA6dcfdfd531e44ae4ac30e8f97e071ab2";
		try{
			Sid sid = new Sid(sidString);
			assertTrue(sid != null);
		}catch(Exception e){
			fail("invalid validation");
		}
	}

}
