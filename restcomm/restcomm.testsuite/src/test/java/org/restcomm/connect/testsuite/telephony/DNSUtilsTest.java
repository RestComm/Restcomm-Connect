package org.restcomm.connect.testsuite.telephony;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.restcomm.connect.commons.util.DNSUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DNSUtils.class})
public class DNSUtilsTest {

    @Test
    public void testGetHostName() throws UnknownHostException {
        PowerMockito.mockStatic(DNSUtils.class);

        final String domainName = "testdomain2.restcomm.com";
        final InetAddress expectedInetAddress = DNSUtils.getByName("127.0.0.1");
        final String expectedIP = "127.0.0.1";

        PowerMockito.when(DNSUtils.getByName(domainName))
                .thenReturn(expectedInetAddress);
        
        String actualValue = DNSUtils.getByName(domainName).getHostAddress();
        System.out.println("actualValue: "+actualValue+" expectedIP: "+expectedIP);
        assertEquals("Get Desired IP for any domain name: ", expectedIP, actualValue);
    }
}