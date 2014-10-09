package org.mobicents.servlet.restcomm.rvd;

import java.io.File;

import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

//@RunWith(Arquillian.class)
public class RvdControllerTest {

    //@Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class);
        archive.addClass(BuildService.class);
        archive.addPackage("org.mobicents.servlet.restcomm.rvd.model");
        archive.addAsLibraries( Maven.resolver().resolve("com.google.code.gson:gson:2.1").withTransitivity().asFile() );
        archive.addAsLibraries( Maven.resolver().resolve("commons-io:commons-io:2.4").withTransitivity().asFile() );
        archive.addAsLibraries( Maven.resolver().resolve("log4j:log4j:1.2.16").withTransitivity().asFile() );
        
        archive.addAsResource(new File("src/test/resources/BuildServiceTest/state"), ArchivePaths.create("/resources/BuildServiceTest/state"));

        archive.as(ZipExporter.class).exportTo(new File("/tmp/BuildServiceTest.war"), true);
        
        return archive;    
    }
}
