/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
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
                                + ") Mobicents Restcomm " + releaseVersion
                                + " (build: Git Hash=" + releaseRevision
                                + " date=" + releaseDate + ")");
                        logger.info(releaseName + " Mobicents Restcomm "
                                + releaseVersion + " (build: Git Hash="
                                + releaseRevision + " date=" + releaseDate
                                + ") Started.");
                    } else {
                        logger.warn("Unable to extract the version of Mobicents Restcomm currently running");
                    }
                    if (releaseDisclaimer != null) {
                        logger.info(releaseDisclaimer);
                    }
                } else {
                    logger.warn("Unable to extract the version of Mobicents Restcomm currently running");
                }
            } catch (IOException e) {
                logger.warn(
                        "Unable to extract the version of Mobicents Restcomm currently running",
                        e);
            }
        }
    }

    public static String getFullVersion() {
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

                return releaseVersion;
            }
        } catch (Exception e) {
            logger.warn(
                    "Unable to extract the version of Mobicents Sip Servlets currently running",
                    e);
        }
        return null;
    }
}
