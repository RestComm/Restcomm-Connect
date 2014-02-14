package org.mobicents.servlet.restcomm.rvd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.mobicents.servlet.restcomm.rvd.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDirectoryAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.WavFileItem;

@Path("/manager/projects")
public class RvdManager {

    static final Logger logger = Logger.getLogger(BuildService.class.getName());

    @Context
    ServletContext servletContext;
    private ProjectService projectService;

    @PostConstruct
    void init() {
        projectService = new ProjectService(servletContext);
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listProjects(@Context HttpServletRequest request) {

        List<ProjectItem> items;
        try {
            items = projectService.getAvailableProjects();
            ProjectService.fillStartUrlsForProjects(items, request);

        } catch (BadWorkspaceDirectoryStructure e) {
            e.printStackTrace(); // TODO remove this and log the error
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (URISyntaxException e) {
            e.printStackTrace(); // probably caused by a bad project name
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/wavlist")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listWavs(@QueryParam("name") String name) {

        List<WavFileItem> items;
        try {
            if (!projectService.projectExists(name))
                return Response.status(Status.NOT_FOUND).build();
            items = projectService.getWavs(name);
        } catch (BadWorkspaceDirectoryStructure e) {
            e.printStackTrace(); // TODO remove this and log the error
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (ProjectDoesNotExist e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProject(@QueryParam("name") String name) {

        // TODO IMPORTANT!!! sanitize the project name!!

        try {
            projectService.createProject(name);
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (ProjectDirectoryAlreadyExists e) {
            e.printStackTrace();
            return Response.status(Status.CONFLICT).build();
        }

        return Response.ok().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProject(@Context HttpServletRequest request, @QueryParam("name") String projectName) {

        // TODO IMPORTANT!!! sanitize the project name!!

        if (projectName != null && !projectName.equals("")) {
            logger.info("savingProject " + projectName);
            if (projectService.updateProject(request, projectName))
                return Response.ok().build();
            else
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @PUT
    @Path("/rename")
    public Response renameProject(@QueryParam("name") String projectName, @QueryParam("newName") String projectNewName) {

        // TODO IMPORTANT!!! sanitize the project name!!
        if ((projectName != null && !projectName.equals("")) && (projectNewName != null && !projectNewName.equals(""))) {
            try {
                if (projectService.renameProject(projectName, projectNewName))
                    return Response.ok().build();
                else
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (ProjectDoesNotExist e) {
                e.printStackTrace();
                return Response.status(Status.NOT_FOUND).build();
            } catch (ProjectDirectoryAlreadyExists e) {
                e.printStackTrace();
                return Response.status(Status.CONFLICT).build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @DELETE
    @Path("/delete")
    public Response deleteProject(@QueryParam("name") String projectName) {

        // TODO IMPORTANT!!! sanitize the project name!!
        if (projectName != null && !projectName.equals("")) {
            try {
                if (projectService.deleteProject(projectName))
                    return Response.ok().build();
                else
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (ProjectDoesNotExist e) {
                e.printStackTrace();
                return Response.status(Status.NOT_FOUND).build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response openProject(@QueryParam("name") String name, @Context HttpServletRequest request) {

        // TODO CAUTION!!! sanitize name
        // ...

        String workspaceBasePath = projectService.getWorkspaceBasePath();
        File stateFile = new File(workspaceBasePath + File.separator + name + File.separator + "state");
        try {
            FileInputStream stateFileStream = new FileInputStream(stateFile);
            // request.getSession().setAttribute(projectSessionAttribute, name); // mark the open project in the session
            return Response.ok().entity(stateFileStream).build();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return Response.status(Status.BAD_REQUEST).build(); // TODO This is not the correct return code for all cases of error
    }

    @POST
    @Path("/uploadwav")
    public Response uploadWavFile(@QueryParam("name") String projectName, @Context HttpServletRequest request) {
        logger.debug("running /uploadwav");

        try {
            if (request.getHeader("Content-Type") != null && request.getHeader("Content-Type").startsWith("multipart/form-data")) {
                Gson gson = new Gson();
                ServletFileUpload upload = new ServletFileUpload();
                FileItemIterator iterator = upload.getItemIterator(request);
                String projectBase = projectService.getProjectBasePath(projectName);

                JsonArray fileinfos = new JsonArray();

                while (iterator.hasNext()) {
                    FileItemStream item = iterator.next();
                    JsonObject fileinfo = new JsonObject();
                    fileinfo.addProperty("fieldName", item.getFieldName());

                    // is this a file part (talking about multipart requests, there might be parts that are not actual files). They will be ignored
                    if (item.getName() != null) {
                        // copy from temp storage to file in project/wavs directory
                        String wavPathname = projectService.getProjectWavsPath(projectName) + File.separator + item.getName();
                        logger.debug( "Writing wav file to " + wavPathname);
                        FileUtils.copyInputStreamToFile(item.openStream(), new File(wavPathname) );
                        fileinfo.addProperty("name", item.getName());
                        //fileinfo.addProperty("size", size(item.openStream()));
                    }
                    if (item.getName() == null) {
                        logger.warn( "non-file part found in upload");
                        fileinfo.addProperty("value", read(item.openStream()));
                    }
                    fileinfos.add(fileinfo);
                }

                return Response.ok(gson.toJson(fileinfos), MediaType.APPLICATION_JSON).build();

            } else {

                String json_response = "{\"result\":[{\"size\":" + size(request.getInputStream()) + "}]}";
                return Response.ok(json_response,MediaType.APPLICATION_JSON).build();
            }
        } catch ( Exception e /* TODO - use a more specific  type !!! */) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        //return Response.ok().build();
    }

    @DELETE
    @Path("/removewav")
    public Response removeWavFile(@QueryParam("name") String projectName, @QueryParam("filename") String filename, @Context HttpServletRequest request) {
        // !!! Sanitize project name
        String filepath;
        try {
            filepath = projectService.getProjectWavsPath(projectName) + File.separator + filename;
            File wavfile = new File(filepath);
            if ( wavfile.delete() )
                logger.info( "Deleted " + filename + " from " + projectName + " app" );
            else
                logger.warn( "Cannot delete " + filename + " from " + projectName + " app" );
            return Response.ok().build();
        } catch (BadWorkspaceDirectoryStructure e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (ProjectDoesNotExist e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    protected int size(InputStream stream) {
        int length = 0;
        try {
            byte[] buffer = new byte[2048];
            int size;
            while ((size = stream.read(buffer)) != -1) {
                length += size;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return length;

    }

    protected String read(InputStream stream) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
        return sb.toString();

    }

    @POST
    @Path("/build")
    public Response buildProject(@QueryParam("name") String name) {

        // !!! SANITIZE project name

        if (name != null && !name.equals("")) {

            String workspaceBasePath = projectService.getWorkspaceBasePath();
            File projectDir = new File(workspaceBasePath + File.separator + name);

            if (projectDir.exists()) {

                String projectPath = workspaceBasePath + File.separator + name + File.separator;
                File dataDir = new File(projectPath + "data");

                // delete all files in directory
                for (File anyfile : dataDir.listFiles()) {
                    anyfile.delete();
                }

                // and now process state
                try {

                    String state_json = FileUtils.readFileToString(new File(projectPath + "state"), "UTF-8");
                    logger.debug("state: " + state_json);
                    BuildService buildService = new BuildService();
                    buildService.buildProject(state_json, projectPath);

                    return Response.ok().build();

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Status.BAD_REQUEST).build();
    }

}
