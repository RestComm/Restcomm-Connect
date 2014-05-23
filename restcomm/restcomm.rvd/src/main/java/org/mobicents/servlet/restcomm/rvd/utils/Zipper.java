package org.mobicents.servlet.restcomm.rvd.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.utils.exceptions.ZipperException;

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
