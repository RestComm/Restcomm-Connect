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

package org.restcomm.connect.commons;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * @author jderruelle
 * @author pslegr
 * @author gvagenas
 *
 */
public class Version {
    private static Logger logger = Logger.getLogger(Version.class);
    private static VersionEntity versionEntity;

    public static void printVersion() {
        if (logger.isInfoEnabled()) {
            if (versionEntity == null)
                versionEntity = generateVersionEntity();
            String releaseVersion = versionEntity.getVersion();
            String releaseName = versionEntity.getName();
            String releaseDate = versionEntity.getDate();
            String releaseRevision = versionEntity.getRevision();
            String releaseDisclaimer = versionEntity.getDisclaimer();
            if (releaseVersion != null) {
                // Follow the EAP Convention
                // Release ID: JBoss [EAP] 5.0.1 (build:
                // SVNTag=JBPAPP_5_0_1 date=201003301050)
                logger.info("Release ID: Restcomm-Connect " + releaseVersion
                        + " (build: Git Hash=" + releaseRevision
                        + " date=" + releaseDate + ")");
                logger.info(releaseName + " Restcomm-Connect "
                        + releaseVersion + " (build: Git Hash="
                        + releaseRevision + " date=" + releaseDate
                        + ") Started.");
            } else {
                logger.warn("Unable to extract the version of Restcomm-Connect currently running");
            }
            if (releaseDisclaimer != null) {
                logger.info(releaseDisclaimer);
            }
        } else {
            logger.warn("Unable to extract the version of Restcomm-Connect currently running");
        }
    }

    private static VersionEntity generateVersionEntity() {
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
                versionEntity = new VersionEntity(releaseVersion,releaseRevision,releaseName,releaseDate, releaseDisclaimer);
            } else {
                logger.warn("Unable to extract the version of Restcomm-Connect currently running");
            }
        } catch (IOException e) {
            logger.warn("Unable to extract the version of Restcomm-Connect currently running",e);
        }
        return versionEntity;
    }

    public static VersionEntity getVersionEntity() {
        if (versionEntity != null) {
            return versionEntity;
        } else {
            return generateVersionEntity();
        }
    }

    public static String getFullVersion() {
        if (versionEntity == null)
            versionEntity = generateVersionEntity();

        String releaseVersion = versionEntity.getVersion();
        String releaseName = versionEntity.getName();
        String releaseDate = versionEntity.getDate();
        if(releaseDate.equals("${maven.build.timestamp}")) {
            Date date = new Date();
            releaseDate = new SimpleDateFormat("yyyy/MM/dd_HH:mm").format(date);
        }
        String releaseRevision = versionEntity.getRevision();

        return "Release ID: Restcomm-Connect "
                + releaseVersion + " (build: Git Hash="
                + releaseRevision + " date=" + releaseDate + ")";
    }

    public static String getVersion() {
        if (versionEntity == null)
            versionEntity = generateVersionEntity();
        return versionEntity.getVersion();
    }

    public static String getRevision() {
        if (versionEntity == null)
            versionEntity = generateVersionEntity();
        return versionEntity.getRevision();
    }
}
