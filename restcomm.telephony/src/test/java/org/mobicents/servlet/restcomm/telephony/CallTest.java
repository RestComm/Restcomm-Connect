/*
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
package org.mobicents.servlet.restcomm.telephony;

import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;

import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@RunWith(Arquillian.class)
public final class CallTest {
  private static final String version = "1.6.0.GA";
  
  @ArquillianResource
  private Deployer deployer;
  
  private static SipStackTool tool;
  private SipStack receiver;
  private SipPhone phone;
  
  public CallTest() {
    super();
  }
  
  @BeforeClass public static void beforeClass() throws Exception {
    tool = new SipStackTool("CallTest");
  }
  
  @Before public void before() throws Exception {
    receiver = tool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
    phone = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, "sip:+17778889999@127.0.0.1:5070");
  }
  
  @After public void after() throws Exception {
    if(phone != null) {
      phone.dispose();
    }
    if(receiver != null) {
      receiver.dispose();
    }
    deployer.undeploy("CallTest");
  }

  @Ignore @Test public void test() {
    deployer.deploy("CallTest");
  }
	
  @Deployment(name="CallTest", managed=false, testable=false)
  public static WebArchive createWebArchive() {
    final WebArchive archive = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.application:war:" + version)
        .withoutTransitivity().asSingle(WebArchive.class);
    JavaArchive dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.commons:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.dao:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.mgcp:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
            .resolve("com.telestax.servlet:restcomm.http:jar:" + version)
            .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.interpreter:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.sms.api:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.sms:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.telephony:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("commons-configuration:commons-configuration:jar:1.7")
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("jain:jain-mgcp-ri:jar:1.0")
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    archive.delete("/WEB-INF/sip.xml");
    archive.delete("/WEB-INF/conf/restcomm.xml");
    archive.delete("/WEB-INF/data/hsql/restcomm.script");
    archive.addAsWebInfResource("sip.xml");
    archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
	archive.addAsWebInfResource("restcomm.script", "data/hsql/restcomm.script");
	// archive.addAsWebResource("entry.xml");
	// archive.addAsWebResource("sms.xml");
    return archive;
  }
}
