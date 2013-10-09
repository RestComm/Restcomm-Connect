package org.mobicents.servlet.restcomm.http;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.JsonArray;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

@RunWith(Arquillian.class)
public class CallsEndpointTest {

	private static final String version = "6.1.2-TelScale-SNAPSHOT";

	@ArquillianResource
	private Deployer deployer;
	@ArquillianResource
	URL deploymentUrl;

	private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
	private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

	@Test
	public void getCallsList(){
		JsonArray allCalls = RestcommCallsTool.getInstance().getCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
		int allCallsSize = allCalls.size();
		assertTrue(allCallsSize == 442);
	}
	
	@Test
	public void getCallsListFilteredByStatus(){
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("Status", "in-progress");
		JsonArray allCalls = RestcommCallsTool.getInstance().getCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
		
		JsonArray filteredCallsByStatus = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(), 
				adminAccountSid, adminAuthToken, filters);
		
		assertTrue(filteredCallsByStatus.size() > 0);
		assertTrue(allCalls.size() > filteredCallsByStatus.size());
	}

	@Test
	public void getCallsListFilteredBySender(){
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("From", "3021097%");
		JsonArray allCalls = RestcommCallsTool.getInstance().getCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
		
		JsonArray filteredCallsBySender = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(), 
				adminAccountSid, adminAuthToken, filters);
		
		assertTrue(filteredCallsBySender.size() > 0);
		assertTrue(allCalls.size() > filteredCallsBySender.size());
	}

	@Test
	public void getCallsListFilteredByRecipient(){
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("To", "1512600%");
		JsonArray allCalls = RestcommCallsTool.getInstance().getCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
		
		JsonArray filteredCallsByRecipient = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(), 
				adminAccountSid, adminAuthToken, filters);
		
		assertTrue(filteredCallsByRecipient.size() > 0);
		assertTrue(allCalls.size() > filteredCallsByRecipient.size());
	}
	
	@Test
	public void getCallsListFilteredByStartTime(){
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("StartTime", "2013-09-10");
		JsonArray allCalls = RestcommCallsTool.getInstance().getCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
		
		JsonArray filteredCallsByStartTime = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(), 
				adminAccountSid, adminAuthToken, filters);
		
		assertTrue(filteredCallsByStartTime.size() > 0);
		assertTrue(allCalls.size() > filteredCallsByStartTime.size());
	}
	
	@Test
	public void getCallsListFilteredByParentCallSid(){
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("ParentCallSid", "CA01a09068a1f348269b6670ef599a6e57");
		
		JsonArray filteredCallsByParentCallSid = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(), 
				adminAccountSid, adminAuthToken, filters);
		
		assertTrue(filteredCallsByParentCallSid.size() == 0);
	}
	
	@Test
	public void getCallsListFilteredUsingMultipleFilters(){
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("StartTime", "2013-09-10");
		filters.put("To", "1512600%");
		filters.put("From", "3021097%");
		filters.put("Status", "in-progress");
		
		JsonArray allCalls = RestcommCallsTool.getInstance().getCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
		
		JsonArray filteredCallsUsingMultipleFilters = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(), 
				adminAccountSid, adminAuthToken, filters);
		
		assertTrue(filteredCallsUsingMultipleFilters.size() > 0);
		assertTrue(allCalls.size() > filteredCallsUsingMultipleFilters.size());
	}	
	
	@Deployment(name="ClientsEndpointTest", managed=true, testable=false)
	public static WebArchive createWebArchiveNoGw() {
		final WebArchive archive = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.application:war:" + version)
				.withoutTransitivity().asSingle(WebArchive.class);
		JavaArchive dependency = ShrinkWrapMaven.resolver()
				.resolve("commons-configuration:commons-configuration:jar:1.7")
				.offline().withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("jain:jain-mgcp-ri:jar:1.0")
				.offline().withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("org.mobicents.media.client:mgcp-driver:jar:3.0.0.Final")
				.offline().withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("joda-time:joda-time:jar:2.0")
				.offline().withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.iSpeech:iSpeech:jar:1.0.1")
				.offline().withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.commons:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.dao:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.asr:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.fax:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.tts.acapela:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.tts.api:jar:" + version)
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
				.resolve("com.telestax.servlet:restcomm.telephony.api:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.telephony:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		archive.delete("/WEB-INF/sip.xml");
		archive.delete("/WEB-INF/conf/restcomm.xml");
		archive.delete("/WEB-INF/data/hsql/restcomm.script");
		archive.addAsWebInfResource("sip.xml");
		archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
		archive.addAsWebInfResource("restcomm_with_Data.script", "data/hsql/restcomm.script");
		return archive;
	}
	
}
