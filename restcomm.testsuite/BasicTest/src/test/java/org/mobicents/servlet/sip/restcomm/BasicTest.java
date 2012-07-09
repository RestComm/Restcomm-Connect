/**
 * 
 */
package org.mobicents.servlet.sip.restcomm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.CallFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Call;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
@RunWith(Arquillian.class)
public class BasicTest {

	@ArquillianResource
	private Deployer deployer;

	//TODO: Find a better way to start/stop MMS
	static String resourcesDir = "/data/devWorkspace/eclipse/eclipseMSS/localWorkspace/restcomm/restcomm.testsuite/resources/";

	@BeforeClass
	public static void beforeClass(){
		// using ProcessBuilder to spawn an process
		ProcessBuilder pb = new ProcessBuilder( "/bin/bash", "-c", resourcesDir+"/mobicents-media-server/bin/run.sh");

		// set up the working directory.
		// Note the method is called "directory" not "setDirectory"
		pb.directory( new File( resourcesDir+"/mobicents-media-server/bin/" ) );

		// merge child's error and normal output streams.
		// Note it is not called setRedirectErrorStream.
		pb.redirectErrorStream( true );
		
		try {
			Process proc = pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void afterClass(){
		/*
		 *In order to kill the previously create process and its childs create a kill.sh script that will contain the following:
		 *    #!/bin/bash
		 *    pkill -f run.sh
		 */
		
		try {
			Process proc = new ProcessBuilder("/bin/bash", resourcesDir+"/kill.sh").start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Deployment (name="restcomm", managed=false, testable=false)
	public static WebArchive createTestArchive()
	{
		MavenDependencyResolver resolver = DependencyResolvers
				.use(MavenDependencyResolver.class)
				.loadMetadataFromPom("pom.xml");

		File srcDir = new File(resourcesDir+"/restcomm.war/");

		//Load archive from WAR file. Place WAR file src/test/resources directory
		//		WebArchive archive = ShrinkWrap.create(ZipImporter.class, "restcomm.war")
		//				.importFrom(new File("src/test/resources/restcomm.war")).as(WebArchive.class);

		//Load archive from exploded directory
		WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
		archive.as(ExplodedImporter.class).importDirectory(srcDir);

		//		System.out.println(archive.toString(true));
		return archive;
	}

	@Test 
	public void testSayVerb() throws InterruptedException, TwilioRestException {

		//Deploy application archive to the container.
		deployer.deploy("restcomm");

		//		assertTrue(true);

		final TwilioRestClient client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
				"77f8c12cc7b8f8423e5c38b035249166");
		final Account account = client.getAccount();
		final CallFactory factory = account.getCallFactory();
		final Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("To", "+15126002188");
		parameters.put("From", "(512) 600-2188");
		parameters.put("Url", "http://192.168.1.106:8080/restcomm/tests/SayVerb");
		final Call call = factory.create(parameters);
		wait(5 * 1000);
		call.hangup();
	}
}
