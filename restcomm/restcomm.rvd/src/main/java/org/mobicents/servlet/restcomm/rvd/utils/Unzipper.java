package org.mobicents.servlet.restcomm.rvd.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
              String filePathname = outputDirectory.getPath() + File.separator + fileName;

              logger.debug("creating new file from zip: " + filePathname);
              FileOutputStream fileEntryStream = new FileOutputStream(new File(filePathname));

              IOUtils.copy(zipInputStream, fileEntryStream);

               fileEntryStream.close();
               zipEntry = zipInputStream.getNextEntry();
           }
           zipInputStream.closeEntry();

       } catch(IOException ex) {
          ex.printStackTrace();
       }
    }
}
