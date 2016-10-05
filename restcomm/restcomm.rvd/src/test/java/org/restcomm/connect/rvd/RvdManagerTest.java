package org.restcomm.connect.rvd;

import java.io.File;
import java.net.URL;

import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.restcomm.connect.rvd.bootstrap.RvdRestApplication;
import org.restcomm.connect.rvd.http.resources.ProjectRestService;

//@RunWith(Arquillian.class)
public class RvdManagerTest {
    
    //@ArquillianResource
    URL deploymentUrl;
    
    //@Deployment (testable = false)
    public static WebArchive createDeployment() {
       WebArchive archive = ShrinkWrap.create(WebArchive.class,"restcomm-rvd.war");       
       
       archive.addAsLibraries( Maven.resolver().resolve("com.google.code.gson:gson:2.1").withTransitivity().asFile() );
       archive.addAsLibraries( Maven.resolver().resolve("com.sun.jersey:jersey-server:1.13").withTransitivity().asFile() );
       archive.addAsLibraries( Maven.resolver().resolve("com.sun.jersey:jersey-client:1.13").withTransitivity().asFile() );
       archive.addAsLibraries( Maven.resolver().resolve("com.sun.jersey:jersey-servlet:1.13").withTransitivity().asFile() );
//       archive.addAsLibraries( Maven.resolver().resolve("com.sun.jersey:jersey-json:1.13").withTransitivity().asFile() );
//       archive.addAsLibraries( Maven.resolver().resolve("com.sun.jersey:jersey-multipart:1.13").withTransitivity().asFile() );
       archive.addAsLibraries( Maven.resolver().resolve("commons-io:commons-io:2.4").withTransitivity().asFile() );
       
       archive.addAsLibraries( Maven.resolver().resolve("com.thoughtworks.xstream:xstream:1.4.5").withTransitivity().asFile() );
       archive.addAsLibraries( Maven.resolver().resolve("org.apache.httpcomponents:httpclient:4.3.1").withTransitivity().asFile() );
       archive.addAsLibraries( Maven.resolver().resolve("commons-fileupload:commons-fileupload:1.3").withTransitivity().asFile() );
       archive.addAsLibraries( Maven.resolver().resolve("log4j:log4j:1.2.16").withTransitivity().asFile() );

       archive.setWebXML(new File("src/main/webapp/WEB-INF/web.xml"));
       archive.addClass(RvdRestApplication.class);
       archive.addClass(ProjectRestService.class);
       archive.addPackage("org.restcomm.connect.rvd");
       archive.addPackage("org.restcomm.connect.rvd.exceptions");
       archive.addPackage("org.restcomm.connect.rvd.model.client");
              
       //archive.addAsWebResource(new File("src/main/webapp/workspace/_proto/state"), ArchivePaths.create("/workspace/_proto/state" ));
       //archive.addAsWebResource(new File("src/main/webapp/workspace/_proto/data/project"), ArchivePaths.create("/workspace/_proto/data/project" ));
       //archive.addAsWebResource(new File("src/main/webapp/workspace/_proto/data/start.node"), ArchivePaths.create("/workspace/_proto/data/start.node" ));
       //archive.addAsWebResource(new File("src/main/webapp/workspace/_proto/data/start.step1"), ArchivePaths.create("/workspace/_proto/data/start.step1" ));
       //archive.addAsWebResource(new File("src/main/webapp/workspace/_proto/wavs/alert.wav"), ArchivePaths.create("/workspace/_proto/wavs/alert.wav" ));
       
       //archive.addAsWebResources(new File("src/main/webapp/workspace"),"asdf");
       //WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");  
       archive.merge(ShrinkWrap
           .create(GenericArchive.class)
           .as(ExplodedImporter.class)  
           .importDirectory("src/test/resources/workspace").as(GenericArchive.class),  
           "/workspace", Filters.includeAll());      
       
       // We want to have a look inside...
       archive.as(ZipExporter.class).exportTo(new File("/tmp/exportedRvdTest.war"), true);

       return archive;
    }    
    
    public RvdManagerTest()  {
    }

    /*
    @Test
    public void testCreateValidProject() throws Exception {
        String projectName = "newProject";
        
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
        
        ClientResponse res = webResource.path("services/manager/projects/").queryParam("name", projectName).put(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());
        
    }
    
    @Test
    public void testOpenProject() throws Exception {
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
        
        ClientResponse res = webResource.path("services/manager/projects").queryParam("name", "ExistingProject").get(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());
        JSONObject project_object = res.getEntity(JSONObject.class);
        Assert.assertEquals("Project JSON state seems invalid", "start", project_object.getString("startNodeName"));
    }
    
    
    @Test 
    public void testUpdateProject() throws Exception {
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
        
        String projectName = "UpdatedProject";
        // get the project
        ClientResponse res = webResource.path("services/manager/projects").queryParam("name", projectName).get(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());
        JSONObject project_object = res.getEntity(JSONObject.class);
        // change something and update it
        project_object.put("startNodeName", "renamed_module");
        res = webResource.path("services/manager/projects")
                .queryParam("name", projectName)
                .type("application/json")
                .post(ClientResponse.class, project_object.toString());
        Assert.assertEquals(200, res.getStatus());
        // get the project again
        res = webResource.path("services/manager/projects").queryParam("name", projectName).get(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());
        // make the sure the property has changed
        project_object = res.getEntity(JSONObject.class);
        Assert.assertEquals("Value has not updated", "renamed_module", project_object.getString("startNodeName"));
    }
    
    
    // generates a warning when the test finishes: "SEVERE: The web application [/restcomm-rvd] created a ThreadLocal with key of type [com.google.gson.Gson$1] (value [com.google.gson.Gson$1@7b5e8a9e]) and a value of type [java.util.HashMap] (value [{}]) but failed to remove it when the web application was stopped. This is very likely to create a memory leak."
    @Test
    public void listProjects() throws Exception {
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource("http://localhost:8080/restcomm-rvd/");

        ClientResponse res = webResource.path("services/manager/projects/list").get(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus()); 
        JSONArray project_list = res.getEntity(JSONArray.class);
        Assert.assertTrue(project_list.length() > 0);
    } 
    
    
    // generates a warning too
    @Test
    public void listProjectWavs() throws Exception {
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
        String projectName = "ExistingProject";
        
        ClientResponse res = webResource.path("services/manager/projects/wavlist")
                .queryParam("name", projectName)
                .get(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus()); 
        JSONArray project_wavlist = res.getEntity(JSONArray.class);
        Assert.assertTrue("An array with some wav items in it expected", project_wavlist.length() > 0);
    }      
    
    
    @Test
    public void testUploadWav() throws Exception {  
        String projectName = "ExistingProject";
        String wavFilename = "new.wav";
        
        // create a mock file to upload
        FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
        String value = "This is a test wav file";
        FormDataContentDisposition dispo = FormDataContentDisposition
                .name("file")
                .fileName(wavFilename)
                .size(value.getBytes().length)
                .build();
        FormDataBodyPart bodyPart = new FormDataBodyPart(dispo, value);
        formDataMultiPart.bodyPart(bodyPart);
        
        // upload the file and test 
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
        ClientResponse res = webResource.path("services/manager/projects/uploadwav")
                .queryParam("name", projectName)
                .type(MediaType.MULTIPART_FORM_DATA)
                .post(ClientResponse.class, formDataMultiPart);
        Assert.assertEquals(200, res.getStatus()); 
        Assert.assertEquals("Malformed response for uploading or file not uploaded", wavFilename, res.getEntity(JSONArray.class).getJSONObject(0).getString("name"));
    }
    
    
    @Test
    public void testRemoveWav() throws Exception {
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
        String projectName = "ExistingProject";
        String wavFilename = "alert.wav";
        
        ClientResponse res = webResource.path("services/manager/projects/removewav")
                .queryParam("name", projectName)
                .queryParam("filename", wavFilename)
                .delete(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());  
    }
    
    
    
    @Test
    public void testRemoveNonExistingWav() throws Exception {
        String projectName = "ExistingProject";
        String wavFilename = "nonexisting-filename.wav";
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
        
        ClientResponse res = webResource.path("services/manager/projects/removewav")
                .queryParam("name", projectName)
                .queryParam("filename", wavFilename)
                .delete(ClientResponse.class);
        Assert.assertEquals(404, res.getStatus());  
    }    
    
    
    @Test
    public void testCreateExistingProject() throws Exception {
        String projectName = "ExistingProject";
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
                
        ClientResponse res = webResource.path("services/manager/projects").queryParam("name", projectName).put(ClientResponse.class);
        Assert.assertEquals(409, res.getStatus());  
    }
    
    
    @Test
    public void testRenameProject() throws Exception {
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
        
        ClientResponse res = webResource.path("services/manager/projects/rename")
                .queryParam("name", "ToRenameProject")
                .queryParam("newName", "RenamedProject")
                .put(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());       
    }    
    
    
    @Test
    public void testDeleteProject() throws Exception {
        String projectName = "ToDeleteProject";
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
        
        ClientResponse res = webResource.path("services/manager/projects/delete").queryParam("name", projectName).delete(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());  
    }  
    
    @Test
    public void testDeleteNonExistingProject() throws Exception {
        String projectName = "NonExistingProject";
        Client client = RestTesterClient.getInstance().getClient();
        WebResource webResource = client.resource(deploymentUrl.toString());
        
        ClientResponse res = webResource.path("services/manager/projects/delete").queryParam("name", projectName).delete(ClientResponse.class);
        Assert.assertEquals(404, res.getStatus());  
    }  
*/
}