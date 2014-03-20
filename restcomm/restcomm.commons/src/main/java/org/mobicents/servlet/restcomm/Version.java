/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.servlet.restcomm;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * @author jderruelle
 * @author pslegr
 *
 */
public class Version {
    private static Logger logger = Logger.getLogger(Version.class);

    public static void printVersion() {
        if (logger.isInfoEnabled()) {
            Properties releaseProperties = new Properties();
            try {
                InputStream in = Version.class
                        .getResourceAsStream("release.properties");
                if (in != null) {
                    releaseProperties.load(in);
                    in.close();
                    String releaseVersion = releaseProperties
                            .getProperty("release.version");
                    String releaseName = releaseProperties
                            .getProperty("release.name");
                    String releaseDate = releaseProperties
                            .getProperty("release.date");
                    String releaseRevision = releaseProperties
                            .getProperty("release.revision");
                    String releaseDisclaimer = releaseProperties
                            .getProperty("release.disclaimer");
                    if (releaseVersion != null) {
                        // Follow the EAP Convention
                        // Release ID: JBoss [EAP] 5.0.1 (build:
                        // SVNTag=JBPAPP_5_0_1 date=201003301050)
                        logger.info("Release ID: (" + releaseName
                                + ") TelScale Restcomm " + releaseVersion
                                + " (build: Git Hash=" + releaseRevision
                                + " date=" + releaseDate + ")");
                        logger.info(releaseName + " TelScale Restcomm "
                                + releaseVersion + " (build: Git Hash="
                                + releaseRevision + " date=" + releaseDate
                                + ") Started.");
                    } else {
                        logger.warn("Unable to extract the version of TelScale Restcomm currently running");
                    }
                    if (releaseDisclaimer != null) {
                        logger.info(releaseDisclaimer);
                    }
                } else {
                    logger.warn("Unable to extract the version of TelScale Restcomm currently running");
                }
            } catch (IOException e) {
                logger.warn(
                        "Unable to extract the version of TelScale Restcomm currently running",
                        e);
            }
        }
    }

    public static String getVersion() {
        Properties releaseProperties = new Properties();
        try {
            InputStream in = Version.class
                    .getResourceAsStream("release.properties");
            if (in != null) {
                releaseProperties.load(in);
                in.close();
                String releaseVersion = releaseProperties
                        .getProperty("release.version");
                String releaseName = releaseProperties
                        .getProperty("release.name");
                String releaseDate = releaseProperties
                        .getProperty("release.date");
                if(releaseDate.equals("${maven.build.timestamp}")) {
                    Date date = new Date();
                    releaseDate = new SimpleDateFormat("yyyy/MM/dd_HH:mm").format(date);
                }
                String releaseRevision = releaseProperties
                        .getProperty("release.revision");

                return "Release ID: (" + releaseName + ") Restcomm "
                        + releaseVersion + " (build: Git Hash="
                        + releaseRevision + " date=" + releaseDate + ")";
            }
        } catch (Exception e) {
            logger.warn(
                    "Unable to extract the version of Mobicents Sip Servlets currently running",
                    e);
        }
        return null;
    }
}
