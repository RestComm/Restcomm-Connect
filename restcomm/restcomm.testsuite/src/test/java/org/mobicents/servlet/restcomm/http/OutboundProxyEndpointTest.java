/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
	
package org.mobicents.servlet.restcomm.http;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.JsonObject;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@RunWith(Arquillian.class)
public class OutboundProxyEndpointTest {

    private final static Logger logger = Logger.getLogger(OutboundProxyEndpointTest.class.getName());

    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @Test
    public void getProxiesTest() {
        JsonObject proxiesJsonObject = OutboundProxyTool.getInstance().getProxies(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken);

        String activeProxy = proxiesJsonObject.get("ActiveProxy").getAsString();
        String primaryProxy = proxiesJsonObject.get("PrimaryProxy").getAsString();
        String fallbackProxy = proxiesJsonObject.get("FallbackProxy").getAsString();
        Boolean usingFallbackProxy = proxiesJsonObject.get("UsingFallBackProxy").getAsBoolean();
        Boolean allowFallbackToPrimary = proxiesJsonObject.get("AllowFallbackToPrimary").getAsBoolean();

        assertTrue(!usingFallbackProxy);
        assertTrue(allowFallbackToPrimary);
        assertTrue(activeProxy.equalsIgnoreCase(primaryProxy));
        assertTrue(fallbackProxy.equalsIgnoreCase("127.0.0.1:5090"));
    }

    @Test
    public void switchProxyTest() {
        JsonObject proxiesJsonObject = OutboundProxyTool.getInstance().getProxies(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken);

        String activeProxy = proxiesJsonObject.get("ActiveProxy").getAsString();
        String primaryProxy = proxiesJsonObject.get("PrimaryProxy").getAsString();
        String fallbackProxy = proxiesJsonObject.get("FallbackProxy").getAsString();
        Boolean usingFallbackProxy = proxiesJsonObject.get("UsingFallBackProxy").getAsBoolean();
        Boolean allowFallbackToPrimary = proxiesJsonObject.get("AllowFallbackToPrimary").getAsBoolean();

        assertTrue(!usingFallbackProxy);
        assertTrue(allowFallbackToPrimary);
        assertTrue(activeProxy.equalsIgnoreCase(primaryProxy));
        assertTrue(fallbackProxy.equalsIgnoreCase("127.0.0.1:5090"));
        
        //Switch to fallback
        JsonObject switchProxyJsonObject = OutboundProxyTool.getInstance().switchProxy(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        activeProxy = switchProxyJsonObject.get("ActiveProxy").getAsString();
        assertTrue(activeProxy.equalsIgnoreCase(fallbackProxy));

        JsonObject activeProxyJsonObject = OutboundProxyTool.getInstance().getActiveProxy(deploymentUrl.toString(), adminAccountSid, adminAuthToken);        
        activeProxy = activeProxyJsonObject.get("ActiveProxy").getAsString();
        assertTrue(activeProxy.equalsIgnoreCase(fallbackProxy));
        
        //Switch back to primary
        switchProxyJsonObject = OutboundProxyTool.getInstance().switchProxy(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        activeProxy = switchProxyJsonObject.get("ActiveProxy").getAsString();
        assertTrue(activeProxy.equalsIgnoreCase(primaryProxy));
        
        activeProxyJsonObject = OutboundProxyTool.getInstance().getActiveProxy(deploymentUrl.toString(), adminAccountSid, adminAuthToken);        
        activeProxy = activeProxyJsonObject.get("ActiveProxy").getAsString();
        assertTrue(activeProxy.equalsIgnoreCase(primaryProxy));
    }
    
    @Deployment(name = "OutboundProxyEndpointTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        final WebArchive archive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        JavaArchive dependency = ShrinkWrapMaven.resolver().resolve("commons-configuration:commons-configuration:jar:1.7")
                .offline().withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("jain:jain-mgcp-ri:jar:1.0").offline().withoutTransitivity()
                .asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("org.mobicents.media.client:mgcp-driver:jar:3.0.0.Final").offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("joda-time:joda-time:jar:2.0").offline().withoutTransitivity()
                .asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.iSpeech:iSpeech:jar:1.0.1").offline().withoutTransitivity()
                .asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.commons:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.dao:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.asr:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.fax:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.tts.acapela:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.tts.api:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.mgcp:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.http:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.interpreter:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.sms.api:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.sms:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.telephony.api:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.telephony:jar:" + version).offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
//        archive.delete("/WEB-INF/sip.xml");
//        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
//        archive.addAsWebInfResource("sip.xml");
//        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }
}
