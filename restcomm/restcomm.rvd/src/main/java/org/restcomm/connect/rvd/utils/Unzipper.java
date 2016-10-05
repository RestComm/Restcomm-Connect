package org.restcomm.connect.rvd.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class Unzipper {
    static final Logger logger = Logger.getLogger(Unzipper.class.getName());

    File outputDirectory;

    public Unzipper(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void unzip(InputStream zipStream) {
        try{
           ZipInputStream zipInputStream = new ZipInputStream(zipStream);

           ZipEntry zipEntry = zipInputStream.getNextEntry();

           while ( zipEntry != null ) {
              String fileName = zipEntry.getName();
              String pathname = outputDirectory.getPath() + File.separator + fileName;

              String destinationDirPath = FilenameUtils.getFullPath(pathname);
              File destinationDir = new File(destinationDirPath);

              // create the destination directory if it does not exist (works for both file and dir entries)
              if (!destinationDir.exists()) {
                  if(logger.isDebugEnabled()) {
                      logger.debug("creating new directory from zip: " + pathname);
                  }
                  destinationDir.mkdirs();
              }

              if (!zipEntry.isDirectory()) {
                  if(logger.isDebugEnabled()) {
                      logger.debug("creating new file from zip: " + pathname);
                  }
                  FileOutputStream fileEntryStream = new FileOutputStream(new File(pathname));
                  IOUtils.copy(zipInputStream, fileEntryStream);
                  fileEntryStream.close();
              }

              zipEntry = zipInputStream.getNextEntry();
           }
           zipInputStream.closeEntry();

       } catch(IOException ex) {
          ex.printStackTrace();
       }
    }
}
