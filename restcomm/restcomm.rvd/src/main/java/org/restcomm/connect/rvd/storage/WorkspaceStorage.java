package org.restcomm.connect.rvd.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.restcomm.connect.rvd.model.CallControlInfo;
import org.restcomm.connect.rvd.model.ModelMarshaler;
import org.restcomm.connect.rvd.storage.exceptions.StorageEntityNotFound;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

public class WorkspaceStorage {

    String rootPath;
    ModelMarshaler marshaler;

    public WorkspaceStorage(String rootPath, ModelMarshaler marshaler ) {
        this.rootPath = rootPath;
        this.marshaler = marshaler;
    }

    public boolean entityExists(String entityName, String relativePath) {
        if ( !relativePath.startsWith( "/") )
            relativePath = File.separator + relativePath;
        String pathname = rootPath + relativePath + File.separator + entityName;
        File file = new File(pathname);
        return file.exists();
    }

    public <T> T loadEntity(String entityName, String relativePath, Class<T> entityClass) throws StorageException {
        // make sure relativePaths (path within the workspace) start with "/"
        if ( !relativePath.startsWith( "/") )
            relativePath = File.separator + relativePath;

        String pathname = rootPath + relativePath + File.separator + entityName;

        File file = new File(pathname);
        if ( !file.exists() )
            throw new StorageEntityNotFound("File " + file.getPath() + " does not exist");

        String data;
        try {
            data = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
            T instance = marshaler.toModel(data, entityClass);
            return instance;
        } catch (IOException e) {
            throw new StorageException("Error loading file " + file.getPath(), e);
        }
    }

    public <T> T loadEntity(String entityName, String relativePath, Type gsonType) throws StorageException {
        // make sure relativePaths (path within the workspace) start with "/"
        if ( !relativePath.startsWith( "/") )
            relativePath = File.separator + relativePath;

        String pathname = rootPath + relativePath + File.separator + entityName;

        File file = new File(pathname);
        if ( !file.exists() )
            throw new StorageEntityNotFound("File " + file.getPath() + " does not exist");

        String data;
        try {
            data = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
            T instance = marshaler.toModel(data, gsonType);
            return instance;
        } catch (IOException e) {
            throw new StorageException("Error loading file " + file.getPath(), e);
        }
    }

    public InputStream loadStream(String entityName, String relativePath) throws StorageException {
        if ( !relativePath.startsWith( "/") )
            relativePath = File.separator + relativePath;
        String pathname = rootPath + relativePath + File.separator + entityName;

        File file = new File(pathname);
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new StorageEntityNotFound("File " + file.getPath() + " does not exist");
        }
    }



    public void storeEntity(Object entity, Class<?> entityClass, String entityName, String relativePath ) throws StorageException {
        if ( !relativePath.startsWith("/") )
            relativePath = "/" + relativePath;

        String pathname = rootPath + relativePath + File.separator + entityName;
        File file = new File(pathname);
        String data = marshaler.getGson().toJson(entity, entityClass);
        try {
            FileUtils.writeStringToFile(file, data, "UTF-8");
        } catch (IOException e) {
            throw new StorageException("Error creating file in storage: " + file, e);
        }
    }

    public void storeEntity(Object entity, String entityName, String relativePath ) throws StorageException {
        if ( !relativePath.startsWith("/") )
            relativePath = "/" + relativePath;

        String pathname = rootPath + relativePath + File.separator + entityName;
        File file = new File(pathname);
        String data = marshaler.getGson().toJson(entity);
        try {
            FileUtils.writeStringToFile(file, data, "UTF-8");
        } catch (IOException e) {
            throw new StorageException("Error creating file in storage: " + file, e);
        }
    }

    public void removeEntity(String entityName, String relativePath) {
        if ( !relativePath.startsWith("/") )
            relativePath = "/" + relativePath;
        String pathname = rootPath + relativePath + File.separator + entityName;
        File file = new File(pathname);
        FileUtils.deleteQuietly(file);
    }


    public void storeFile( Object item, Class<?> itemClass, File file) throws StorageException {
        String data;
        data = marshaler.getGson().toJson(item, itemClass);

        try {
            FileUtils.writeStringToFile(file, data, "UTF-8");
        } catch (IOException e) {
            throw new StorageException("Error creating file in storage: " + file, e);
        }
    }

    public String loadEntityString(String entityName, String relativePath) throws StorageException {
        if ( !relativePath.startsWith( "/") )
            relativePath = File.separator + relativePath;

        String pathname = rootPath + relativePath + File.separator + entityName;

        File file = new File(pathname);
        if ( !file.exists() )
            throw new StorageEntityNotFound("File " + file.getPath() + " does not exist");

        String data;
        try {
            data = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
            return data;
        } catch (IOException e) {
            throw new StorageException("Error loading file " + file.getPath(), e);
        }
    }

    public void storeEntityString(String entityString, String entityName, String relativePath) throws StorageException {
        if ( !relativePath.startsWith("/") )
            relativePath = "/" + relativePath;

        String pathname = rootPath + relativePath + File.separator + entityName;
        File file = new File(pathname);
        try {
            FileUtils.writeStringToFile(file, entityString, "UTF-8");
        } catch (IOException e) {
            throw new StorageException("Error creating file in storage: " + file, e);
        }
    }

    public void storeBinaryFile(File sourceFile, String entityName, String relativePath) throws StorageException {
        if ( !relativePath.startsWith("/") )
            relativePath = "/" + relativePath;

        String pathname = rootPath + relativePath + File.separator + entityName;
        //File destFile = new File(getProjectBasePath(projectName) + File.separator + RvdConfiguration.PACKAGING_DIRECTORY_NAME + File.separator + "app.zip");
        File destFile = new File( pathname );
        try {
            FileUtils.copyFile(sourceFile, destFile);
            FileUtils.deleteQuietly(sourceFile);
        } catch (IOException e) {
            throw new StorageException("Error copying binary file into project", e);
        }
    }

    public InputStream loadBinaryFile(String projectName, String entityName, String relativePath) throws FileNotFoundException {
        if ( !relativePath.startsWith("/") )
            relativePath = "/" + relativePath;
        String pathname = rootPath + relativePath + File.separator + entityName;

        File packageFile = new File( pathname );
        return new FileInputStream(packageFile);
    }

    public static void storeInfo(CallControlInfo info, String projectName, WorkspaceStorage workspaceStorage) throws StorageException {
        workspaceStorage.storeEntity(info, CallControlInfo.class, "cc", projectName);
    }

    public <T> T loadModelFromXMLFile(String filepath, Class<T> modelClass) throws StorageException {
        File file = new File(filepath);
        return loadModelFromXMLFile(file, modelClass);
    }

    // CAUTION! what happens if the typecasting fails? solve this..
    public <T> T loadModelFromXMLFile(File file, Class<T> modelClass) throws StorageException {
        if ( !file.exists() )
            throw new StorageEntityNotFound("Cannot find file: " + file.getPath() );

        try {
            String data = FileUtils.readFileToString(file, "UTF-8");
            T instance = (T) marshaler.getXStream().fromXML(data);
            return instance;

        } catch (IOException e) {
            throw new StorageException("Error loading model from file '" + file + "'", e);
        }
    }

    public <T> T loadModelFromFile(String filepath, Type gsonType) throws StorageException {
        File file = new File(filepath);
        return loadModelFromFile(file, gsonType);
    }

    public <T> T loadModelFromFile(File file, Type gsonType) throws StorageException {
        if ( !file.exists() )
            throw new StorageEntityNotFound("Cannot find file: " + file.getPath() );

        try {
            String data = FileUtils.readFileToString(file, "UTF-8");
            T instance = marshaler.getGson().fromJson(data, gsonType);
            return instance;

        } catch (IOException e) {
            throw new StorageException("Error loading model from file '" + file + "'", e);
        }
    }



}
