package org.restcomm.connect.rvd.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.utils.exceptions.ZipperException;

public class Zipper {
    static final Logger logger = Logger.getLogger(Zipper.class.getName());

    ZipOutputStream zipOut;
    File zipFile;

    public Zipper(File tempFile) throws ZipperException {
        zipFile = tempFile;
        try {
            zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
        } catch (FileNotFoundException e) {
            throw new ZipperException("Error creating zip " + zipFile, e);
        }

    }

    public void addDirectory(String name) throws ZipperException {
        try {
            zipOut.putNextEntry(new ZipEntry(name));
            zipOut.closeEntry();
        } catch (IOException e) {
            throw new ZipperException("Error adding directory " + name + " to zip " + zipFile , e);
        }
    }

    public void addFile(String filepath, InputStream fileStream) throws ZipperException {
        try {
            zipOut.putNextEntry(new ZipEntry(filepath));
            IOUtils.copy(fileStream, zipOut);
            zipOut.closeEntry();
        } catch (IOException e) {
            throw new ZipperException("Error adding file " + filepath + " to zip " + zipFile, e);
        }

    }

    public void addFileContent(String filepath, String fileContent) throws ZipperException {
        try {
            zipOut.putNextEntry(new ZipEntry(filepath));
            IOUtils.write(fileContent, zipOut, "UTF-8");
            zipOut.closeEntry();
        } catch (IOException e) {
            throw new ZipperException("Error adding string content to zip " + zipFile, e);
        }

    }

    /**
     * Adds a directory recursively into the zip. Files starting with "." are excluded.
     * @param dirpath An absolute path to the directory to be added. It may contain a trailing slash - "/"
     * @param includeRoot Add the parent directory too in the zip file or just its children
     * @throws ZipperException
     */
    public void addDirectoryRecursively(String dirpath, boolean includeRoot) throws ZipperException {
        File dir = new File(dirpath);
        if ( dir.exists() && dir.isDirectory() ) {
            String dirName = dir.getName();
            String dirParent = dir.getParent();
            if ( includeRoot ) {
                addDirectory(dirName + "/");
                addNestedDirectoryContents(dirParent + "/", dirName);
            } else {
                addNestedDirectoryContents(dirParent + "/" + dirName, "");
            }
        } else {
            throw new ZipperException(dirpath + " is not a directory or does not exist");
        }
    }

    /**
     * Internal use function that implements the recursion logic. What changes throughout the recursion is the childPath that matches the
     * path used while storing in the zip.
     * @param rootPath
     * @param childPath
     * @throws ZipperException
     */
    private void addNestedDirectoryContents(String rootPath, String childPath) throws ZipperException {
        String nestedPath = rootPath + childPath;
        File dir = new File(nestedPath);

        assert dir.isDirectory();

        File[] childrenFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.startsWith(".");
            }
        });

        for ( File file : childrenFiles ) {
            if ( file.isDirectory() ) {
                addDirectory(childPath + "/" + file.getName() + "/");
                addNestedDirectoryContents(rootPath, childPath + "/" + file.getName());
            } else {
                FileInputStream inputStream;
                try {
                    inputStream = new FileInputStream(file);
                    try {
                        addFile(childPath + "/" + file.getName(), inputStream);
                    } finally {
                        inputStream.close();
                    }
                } catch (FileNotFoundException e) {
                    throw new ZipperException("Error adding file " + file + " to zip", e);
                } catch (IOException e ) {
                    throw new ZipperException("Error closingfile " + file + " after adding it to zip", e);
                }

            }
        }
    }

    /**
     * Best effort finish function. If it fails it looks there is more that can be done. We just log
     * the message.
     */
    public void finish() {
        try {
            zipOut.finish();
        } catch (IOException e) {
            logger.warn("Error closing Zipper " + zipFile + ". There is nothing more that can be done.", e);
        }
    }
}
