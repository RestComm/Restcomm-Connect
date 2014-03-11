package org.mobicents.servlet.restcomm.rvd;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;

@RunWith(Arquillian.class)
public class BuildServiceTest {
    
    private String testTempDirectory;
    
    @Deployment
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

    @Before public void  setup() {        
        testTempDirectory = FileUtils.getTempDirectoryPath() + File.separator + "rvdtest-" + Long.toString(System.nanoTime());                
        new File( testTempDirectory).mkdir();
    }
    
    @Test
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
    
    @After public void teardown() throws IOException {
        FileUtils.deleteDirectory( new File(testTempDirectory));
    }

}
