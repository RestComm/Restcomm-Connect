package org.mobicents.servlet.restcomm.http;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.runner.RunWith;

/**
 * @author orestis.tsakiridis@gmail.com - Orestis Tsakiridis
 */
@RunWith(Arquillian.class)
public class OrgIdentityEndpointTest extends EndpointTest {
    protected final static Logger logger = Logger.getLogger(OrgIdentityEndpointTest.class);

    @Deployment(name = "OrgIdentityEndpointTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        logger.info("version");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm_OrgIdentity.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_OrgIdentity", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }
}
