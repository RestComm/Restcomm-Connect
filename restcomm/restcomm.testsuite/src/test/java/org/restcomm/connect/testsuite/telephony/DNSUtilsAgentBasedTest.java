package org.restcomm.connect.testsuite.telephony;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Rule;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.agent.PowerMockAgent;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.restcomm.connect.commons.util.DNSUtils;
import org.restcomm.connect.commons.util.DNSUtilsWrapper;


//@RunWith(PowerMockRunner.class)
@PrepareForTest(DNSUtils.class)
public class DNSUtilsAgentBasedTest {//extends PowerMockTestCase{

	final String domainName = "testdomain2.restcomm.com";
    final String expectedIP = "127.0.0.1";

    //@Rule
    //public PowerMockRule rule = new PowerMockRule();
    
    static {PowerMockAgent.initializeIfNeeded();}
    
    //@Mock
    //private DNSUtils dnsUtils;
    
	/*@Before
    public void setUp() throws Exception {
		PowerMockito.mockStatic(DNSUtils.class);
        PowerMockito.when(DNSUtils.getByName(domainName)).thenReturn(InetAddress.getByName("127.0.0.1"));
	}*/

	@Test
	@PrepareForTest(DNSUtils.class)
    public void testGetHostName() throws UnknownHostException {
        
        PowerMockito.mockStatic(DNSUtils.class);
        PowerMockito.when(DNSUtils.getByName(domainName)).thenReturn(InetAddress.getByName("127.0.0.1"));
        
        String resultedValue = DNSUtilsWrapper.getByName(domainName).getHostAddress();
        System.out.println("resultedValue: "+resultedValue+" expectedIP: "+expectedIP);
        assertEquals("Get Desired IP for any domain name: ", expectedIP, resultedValue);
    }
}