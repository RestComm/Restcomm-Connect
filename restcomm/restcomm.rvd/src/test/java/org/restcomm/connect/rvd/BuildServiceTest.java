package org.restcomm.connect.rvd;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.restcomm.connect.rvd.BuildService;

//@RunWith(Arquillian.class)
public class BuildServiceTest {
    
    private String testTempDirectory;
    
    //@Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class);
        archive.addClass(BuildService.class);
        archive.addPackage("org.restcomm.connect.rvd.model");
        archive.addAsLibraries( Maven.resolver().resolve("com.google.code.gson:gson:2.1").withTransitivity().asFile() );
        archive.addAsLibraries( Maven.resolver().resolve("commons-io:commons-io:2.4").withTransitivity().asFile() );
        archive.addAsLibraries( Maven.resolver().resolve("log4j:log4j:1.2.16").withTransitivity().asFile() );
        
        archive.addAsResource(new File("src/test/resources/BuildServiceTest/state"), ArchivePaths.create("/resources/BuildServiceTest/state"));

        archive.as(ZipExporter.class).exportTo(new File("/tmp/BuildServiceTest.war"), true);
        
        return archive;    
    }

    //@Before 
    public void  setup() {        
        testTempDirectory = FileUtils.getTempDirectoryPath() + File.separator + "rvdtest-" + Long.toString(System.nanoTime());                
        new File( testTempDirectory).mkdir();
    }
    
    //@Test
    public void testBuildProject() throws Exception {
        /*
        BuildService buildService = new BuildService();
        String builtProjectDirectory = testTempDirectory + File.separator + "TestProject/";
        
        InputStream s = getClass().getClassLoader().getResourceAsStream("/resources/BuildServiceTest/state");
        String state_string = IOUtils.toString(s, "UTF-8");
        
        buildService.buildProject(state_string, builtProjectDirectory );
        
        Assert.assertTrue("'project' missing", new File(builtProjectDirectory + "/data/project").exists());
        Assert.assertTrue("'start.node' missing", new File(builtProjectDirectory + "/data/start.node").exists());
        Assert.assertTrue("'start.step1' missing", new File(builtProjectDirectory + "/data/start.step1").exists());        
        */
    }
    
    //@After 
    public void teardown() throws IOException {
        FileUtils.deleteDirectory( new File(testTempDirectory));
    }

}
