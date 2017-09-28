package org.restcomm.connect.commons.configuration;


import java.net.InetSocketAddress;
import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.configuration.sets.MainConfigurationSet;
import org.restcomm.connect.commons.common.http.SslMode;

public class RestcommConfigurationTest {
    private RestcommConfiguration conf;
    private XMLConfiguration xml;

    public RestcommConfigurationTest() {
        super();
    }

    @Before
    public void before() throws ConfigurationException, MalformedURLException {
        URL url = this.getClass().getResource("/restcomm.xml");
        // String relativePath = "../../../../../../../../restcomm.application/src/main/webapp/WEB-INF/conf/restcomm.xml";
        xml = new XMLConfiguration();
        xml.setDelimiterParsingDisabled(true);
        xml.setAttributeSplittingDisabled(true);
        xml.load(url);
        conf = new RestcommConfiguration(xml);
    }
    
    @Test 
    public void allConfiguraitonSetsAreAvailable() {
        assertNotNull(conf.getMain());
        // add new sets here ...
        // ...
    }

    // Test properties for the 'Main' configuration set 
    @Test
    public void mainSetConfigurationOptionsAreValid() {
        MainConfigurationSet main = conf.getMain();
        assertEquals(SslMode.strict, main.getSslMode());
        assertEquals("127.0.0.1", main.getHostname());
        assertTrue(main.isUseHostnameToResolveRelativeUrls());
        assertEquals(Integer.valueOf(200), main.getDefaultHttpMaxConns());
        assertEquals(Integer.valueOf(20), main.getDefaultHttpMaxConnsPerRoute());
        assertEquals(Integer.valueOf(30000), main.getDefaultHttpTTL());
        assertEquals(2, main.getDefaultHttpRoutes().size());
        InetSocketAddress addr1= new InetSocketAddress("127.0.0.1", 8080);
        assertEquals(Integer.valueOf(10), main.getDefaultHttpRoutes().get(addr1));
        InetSocketAddress addr2= new InetSocketAddress("192.168.1.1", 80);
        assertEquals(Integer.valueOf(60), main.getDefaultHttpRoutes().get(addr2));        
    }
    
    @Test 
    public void validSingletonOperation() {
        // make sure it is created
        RestcommConfiguration.createOnce(xml);
        RestcommConfiguration conf1 = RestcommConfiguration.getInstance();
        assertNotNull(conf1);
        // make sure it's not created again for subsequent calls
        RestcommConfiguration.createOnce(xml);
        RestcommConfiguration conf2 = RestcommConfiguration.getInstance();
        assertTrue( conf1 == conf2 );        
    }

}
