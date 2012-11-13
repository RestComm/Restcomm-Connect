package org.mobicents.servlet.sip.restcomm;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mobicents.servlet.sip.restcomm.http.AccountsEndpointTest;
import org.mobicents.servlet.sip.restcomm.http.ClientsEndpointTest;
import org.mobicents.servlet.sip.restcomm.http.IncomingPhoneNumbersEndpointTest;
import org.mobicents.servlet.sip.restcomm.http.NotificationsEndpointTest;
import org.mobicents.servlet.sip.restcomm.http.OutgoingCallerIdEndpointTest;
import org.mobicents.servlet.sip.restcomm.http.RecordingsEndpointTest;
import org.mobicents.servlet.sip.restcomm.http.SmsEndpointTest;
import org.mobicents.servlet.sip.restcomm.http.TranscriptionsEndpointTest;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */
@RunWith(Suite.class)
@SuiteClasses({
	AccountsEndpointTest.class,
//	AnnouncementEndpointTest.class,
	ClientsEndpointTest.class,
	IncomingPhoneNumbersEndpointTest.class,
	NotificationsEndpointTest.class,
	OutgoingCallerIdEndpointTest.class,
	RecordingsEndpointTest.class,
	SmsEndpointTest.class,
	TranscriptionsEndpointTest.class	
})
public class Testsuite {

}
