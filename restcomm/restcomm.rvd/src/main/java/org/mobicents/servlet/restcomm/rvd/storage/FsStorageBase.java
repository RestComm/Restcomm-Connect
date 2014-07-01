package org.mobicents.servlet.restcomm.rvd.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.packaging.exception.PackagingException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

import com.google.gson.Gson;

public class FsStorageBase {
    private String workspaceBasePath;

    public FsStorageBase(String workspaceBasePath) {
        super();
        this.workspaceBasePath = workspaceBasePath;
    }

    // package-private method
    String getProjectBasePath(String name) {
        return workspaceBasePath + File.separator + name;
    }

    String getWorkspaceBasePath() {
        return workspaceBasePath;
    }

    public void storeProjectFile(String data, String projectName, String path, String filename) throws StorageException {
        File file = new File(getProjectBasePath(projectName) + File.separator + path + File.separator + filename);
        try {
            FileUtils.writeStringToFile(file, data, Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new StorageException("Error storing file '" + file + "' to project '" + projectName + "'", e);
        }
    }

    public String loadProjectFile(String projectName, String path, String filename ) throws StorageException {
        File file = new File(getProjectBasePath(projectName) + File.separator + path + File.separator + filename);
        String data;
        try {
            data = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
            return data;
        } catch (IOException e) {
            throw new StorageException("Error loading file '" + file + "' from project '" + projectName + "'");
        }
    }

    public void storeProjectBinaryFile(File sourceFile, String projectName, String path, String filename ) throws RvdException {
        //if (projectExists(projectName)) {
            File destFile = new File(getProjectBasePath(projectName) + File.separator + RvdSettings.PACKAGING_DIRECTORY_NAME + File.separator + "app.zip");
            try {
                //FileUtils.moveFile(packageFile, destFile);
                FileUtils.copyFile(sourceFile, destFile);
                FileUtils.deleteQuietly(sourceFile);
            } catch (IOException e) {
                throw new PackagingException("Error copying binary file into project", e);
            }
        //} else
          //  throw new ProjectDoesNotExist("Project " + projectName + " does not exist");
    }

    public InputStream getProjectBinaryFile(String projectName, String path, String filename) throws RvdException, FileNotFoundException {
        //if (projectExists(projectName)) {
            File packageFile = new File(getProjectBasePath(projectName) + File.separator + path + File.separator + filename);
            //try {
                return new FileInputStream(packageFile);
            //} catch (FileNotFoundException e) {
            //    throw new AppPackageDoesNotExist("No app package exists for project " + projectName);
            //}
        //} else
          //  throw new ProjectDoesNotExist("Project " + projectName + " does not exist");
    }

    public boolean projectPathExists(String projectName, String path) throws ProjectDoesNotExist {
        if ( ! new File(getProjectBasePath(projectName)).exists() )
            throw new ProjectDoesNotExist();
        if ( new File(getProjectBasePath(projectName) + File.separator + path ).exists() )
            return true;
        return false;
    }

    /**
     * Checks if a file exists inside a project.
     * @param projectName
     * @param path - Example: "ras/rapp"
     * @param filename
     * @return
     */
    public boolean projectFileExists(String projectName, String path, String filename) {
        File file = new File(getProjectBasePath(projectName) + File.separator + path + File.separator + filename);
        if ( file.exists() )
            return true;
        return false;
    }

    public void storeFileToProject(Object item, Class<?> itemClass, String projectName, String path, String filename ) throws StorageException {
        File file = new File(getProjectBasePath(projectName) + File.separator + path + File.separator + filename);
        storeFile( item, itemClass, file);
    }


    private void storeFile( Object item, Class<?> itemClass, File file) throws StorageException {
        Gson gson = new Gson();
        String data;
        data = gson.toJson(item, itemClass);

        try {
            FileUtils.writeStringToFile(file, data, "UTF-8");
        } catch (IOException e) {
            throw new StorageException("Error creating file in storage: " + file, e);
        }
    }

    public void storeFileToProject(Object item, Type gsonType, String projectName, String path, String filename ) throws StorageException {
        File file = new File(getProjectBasePath(projectName) + File.separator + path + File.separator + filename);
        storeFile( item, gsonType, file);
    }


    private void storeFile( Object item, Type gsonType, File file) throws StorageException {
        Gson gson = new Gson();
        String data;
        data = gson.toJson(item, gsonType);
        try {
            FileUtils.writeStringToFile(file, data, "UTF-8");
        } catch (IOException e) {
            throw new StorageException("Error creating file in storage: " + file, e);
        }
    }

    public <T> T loadModelFromProjectFile(String projectName, String path, String filename, Class<T> modelClass) throws StorageException {
        return loadModelFromFile(getProjectBasePath(projectName) + File.separator + path + File.separator + filename, modelClass);
    }


    public <T> T loadModelFromFile(String filepath, Class<T> modelClass) throws StorageException {
        File file = new File(filepath);
        return loadModelFromFile(file, modelClass);
    }

    public <T> T loadModelFromFile(File file, Class<T> modelClass) throws StorageException {
        Gson gson = new Gson();
        try {
            String data = FileUtils.readFileToString(file, "UTF-8");
            T instance = gson.fromJson(data, modelClass);
            return instance;

        } catch (IOException e) {
            throw new StorageException("Error loading model from file '" + file + "'", e);
        }
    }

    public <T> T loadModelFromProjectFile(String projectName, String path, String filename, Type gsonType) throws StorageException {
        return loadModelFromFile(getProjectBasePath(projectName) + File.separator + path + File.separator + filename, gsonType);
    }


    public <T> T loadModelFromFile(String filepath, Type gsonType) throws StorageException {
        File file = new File(filepath);
        return loadModelFromFile(file, gsonType);
    }

    public <T> T loadModelFromFile(File file, Type gsonType) throws StorageException {
        Gson gson = new Gson();
        try {
            String data = FileUtils.readFileToString(file, "UTF-8");
            T instance = gson.fromJson(data, gsonType);
            return instance;

        } catch (IOException e) {
            throw new StorageException("Error loading model from file '" + file + "'", e);
        }
    }

}
