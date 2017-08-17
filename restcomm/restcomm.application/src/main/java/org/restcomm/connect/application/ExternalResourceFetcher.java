/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.connect.application;

import org.apache.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author guilherme.jansen@telestax.com
 */

public class ExternalResourceFetcher {

    private static final Logger logger = Logger.getLogger(ExternalResourceFetcher.class);

    public enum ExternalResource {

        DIALOGIC_JSR309("dlgmsc-5.0-alpha.jar", "Dialogic's JSR309 library", "https://www.dialogic.com/files/jsr-309/4.1/dlgmsc-5.0-alpha.jar", ""),
        DIALOGIC_SMILTYPES("dlgcsmiltypes-4.1.429.jar", "Dialogic's SMIL Types library", "https://www.dialogic.com/files/jsr-309/4.1/dlgcsmiltypes-4.1.429.jar", ""),
        DIALOGIC_MSMLTYPES("msmltypes-4.1.429.jar", "Dialogic's MSML Types library", "https://www.dialogic.com/files/jsr-309/4.1/msmltypes-4.1.429.jar", "");

        private String fileName;
        private String fileDescription;
        private URI fileLocation;
        private File pathToSave;

        ExternalResource(String fileName, String fileDescription, String fileLocation, String pathToSave) {
            this.fileName = fileName;
            this.fileDescription = fileDescription;
            try {
                this.fileLocation = new URI(fileLocation);
            } catch (URISyntaxException e) {
                logger.error("Could not initialize URI with value \""+fileLocation+"\"", e);
            }
            this.pathToSave = new File(pathToSave);
        }

        //TODO resolve local absolute path to files and create iteration combined with FileDownloader
    }
}
