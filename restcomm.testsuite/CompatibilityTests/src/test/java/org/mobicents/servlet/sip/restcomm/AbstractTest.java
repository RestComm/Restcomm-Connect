package org.mobicents.servlet.sip.restcomm;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;

public class AbstractTest {
	private static final String restcommVersion = "1.0.0.CR1-SNAPSHOT";
	
	public static WebArchive createWebArchive(){
		WebArchive archive = ShrinkWrapMaven.resolver()
				.resolve("org.mobicents.servlet.sip:restcomm.core:war:"+restcommVersion)
				.withoutTransitivity().asSingle(WebArchive.class);
		return archive;
	}

	public static WebArchive createWebArchive(String restcommConfig){
		WebArchive archive = createWebArchive();
		
		archive.delete("/WEB-INF/conf/restcomm.xml");
		archive.addAsWebInfResource(restcommConfig, "conf/restcomm.xml");

		return archive;		
	}
	
	public static WebArchive createWebArchive(String restcommConfig, String restcommApp){
		WebArchive archive = createWebArchive(restcommConfig);
		
		archive.addAsWebResource(restcommApp, "demo/"+restcommApp);
		
		return archive;
	}
	
	public String getEndpoint(String deploymentUrl) {
		if (deploymentUrl.endsWith("/")) {
			deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
		return deploymentUrl;
	}
	
	@After
	public void tearDown() throws InterruptedException{
		Thread.sleep(1000);
	}
}
