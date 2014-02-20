package org.mobicents.servlet.restcomm.rvd;

import javax.ws.rs.core.MediaType;

import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.test.framework.JerseyTest;
//import com.sun.jersey.test.framework.util.ApplicationDescriptor;
import com.sun.jersey.test.framework.WebAppDescriptor;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class RvdManagerTest extends JerseyTest {
    
    String wavFilename = "testmock.wav";
    String projectName = "TestProject";

    public RvdManagerTest() throws Exception {
        super(new WebAppDescriptor.Builder("org.mobicents.servlet.restcomm.rvd").contextPath("restcomm-rvd").build());

        //WebAppDescriptor appDescriptor = new WebAppDescriptor.Builder("org.mobicents.servlet.restcomm.rvd").build();

        /*
        ApplicationDescriptor appDescriptor = new ApplicationDescriptor();
                appDescriptor.setContextPath("/restcomm-rvd");
                appDescriptor.setRootResourcePackageName("org.mobicents.servlet.restcomm.rvd");
                System.out.println( "Context path: " + appDescriptor.getContextPath() );
        super.setupTestEnvironment(appDescriptor);
        */
        
    }

    @Test
    public void testCreateValidProject() throws Exception {
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects").queryParam("name", projectName).put(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());  
    }
    
    @Test
    public void testOpenProject() throws Exception {
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects").queryParam("name", projectName).get(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());
        JSONObject project_object = res.getEntity(JSONObject.class);
        Assert.assertEquals("Project JSON state seems invalid", "start", project_object.getString("startNodeName"));
    }
    
    @Test 
    public void testUpdateProject() throws Exception {
        WebResource webResource = resource();
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
    
    @Test
    public void listProjects() throws Exception {
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects/list").get(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus()); 
        JSONArray project_list = res.getEntity(JSONArray.class);
        Assert.assertTrue(project_list.length() > 0);
    }  
    
    @Test
    public void listProjectWavs() throws Exception {
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects/list")
                //.queryParam("name", projectName)
                .get(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus()); 
        JSONArray project_wavlist = res.getEntity(JSONArray.class);
        Assert.assertTrue("An array with some wav items in it expected", project_wavlist.length() > 0);
    }      
    
    @Test
    public void testUploadWav() throws Exception {
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
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects/uploadwav")
                .queryParam("name", projectName)
                .type(MediaType.MULTIPART_FORM_DATA)
                .post(ClientResponse.class, formDataMultiPart);
        Assert.assertEquals(200, res.getStatus()); 
        Assert.assertEquals("Malformed response for uploading or file not uploaded", wavFilename, res.getEntity(JSONArray.class).getJSONObject(0).getString("name"));
    }
    
    @Test
    public void testRemoveWav() throws Exception {
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects/removewav")
                .queryParam("name", projectName)
                .queryParam("filename", wavFilename)
                .delete(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());  
    }
    
    @Test
    public void testRemoveNonExistingWav() throws Exception {
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects/removewav")
                .queryParam("name", projectName)
                .queryParam("filename", "nonexisting-filename.wav")
                .delete(ClientResponse.class);
        Assert.assertEquals(404, res.getStatus());  
    }    
    
    @Test
    public void testCreateExistingProject() throws Exception {
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects").queryParam("name", projectName).put(ClientResponse.class);
        Assert.assertEquals(409, res.getStatus());  
    }
    
    @Test
    public void testRenameProject() throws Exception {
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects/rename")
                .queryParam("name", projectName)
                .queryParam("newName", "RenamedProject")
                .put(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());
        res = webResource.path("services/manager/projects/rename")
                .queryParam("name", "RenamedProject")
                .queryParam("newName", projectName)
                .put(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());         
    }    
    
    @Test
    public void testDeleteProject() throws Exception {
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects/delete").queryParam("name", projectName).delete(ClientResponse.class);
        Assert.assertEquals(200, res.getStatus());  
    }  
    
    @Test
    public void testDeleteNonExistingProject() throws Exception {
        WebResource webResource = resource();
        ClientResponse res = webResource.path("services/manager/projects/delete").queryParam("name", projectName).delete(ClientResponse.class);
        Assert.assertEquals(404, res.getStatus());  
    }  
}