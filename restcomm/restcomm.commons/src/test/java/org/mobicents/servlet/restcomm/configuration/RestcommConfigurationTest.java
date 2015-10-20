package org.mobicents.servlet.restcomm.configuration;


import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;
import org.mobicents.servlet.restcomm.http.SslMode;

public class RestcommConfigurationTest {
    private RestcommConfiguration conf;
    private Configuration xml;

    public RestcommConfigurationTest() {
        super();
    }

    @Before
    public void before() throws ConfigurationException, MalformedURLException {
        URL url = this.getClass().getResource("/restcomm.xml");
        // String relativePath = "../../../../../../../../restcomm.application/src/main/webapp/WEB-INF/conf/restcomm.xml";
        xml = new XMLConfiguration(url);
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
        assertTrue( main.getSslMode().equals(SslMode.strict));
        assertTrue( main.getHostname().equals(""));
        assertTrue( main.isUseHostnameToResolveRelativeUrls() == true );
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
