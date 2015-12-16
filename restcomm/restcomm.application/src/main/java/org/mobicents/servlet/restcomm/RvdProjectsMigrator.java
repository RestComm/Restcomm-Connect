/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm;

import java.io.File;
import java.io.FileWriter;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

/**
 * The goal of this class is to generate an Application entity inside the
 * database for each RVD project located inside its workspace.
 * Also, apply the new naming convention on project directories inside
 * the workspace, based on a new {@link org.mobicents.servlet.restcomm.entities.Sid.Type.PROJECT}
 * generated to each entry.
 *
 * @author guilherme.jansen@telestax.com
 */
public class RvdProjectsMigrator {

    private static final Logger logger = Logger.getLogger(RvdProjectsMigrator.class);
    private static final String separator = "--------------------------------------\n";
    private RvdProjectsMigrationHelper migrationHelper;
    private List<String> projectNames;
    private boolean migrationSucceeded;
    private Integer errorCode;
    private String logPath;


    public RvdProjectsMigrator(ServletContext servletContext, Configuration configuration) throws Exception {
        this.migrationHelper = new RvdProjectsMigrationHelper(servletContext, configuration);
        this.migrationSucceeded = true;
        this.logPath = servletContext.getRealPath("/") + "../../../"; // Equivalent to RESTCOMM_HOME
    }

    public void executeMigration() throws Exception {
        // Ensure the migration needs to be executed
        if (!migrationHelper.isMigrationEnabled() || migrationHelper.isMigrationSucceeded()) {
            return;
        }
        String beginning = getTimeStamp();
        logger.info("Starting workspace migration at " + beginning);
        storeLogMessage(separator + "Starting workspace migration at " + beginning + "\n" + separator);
        storeNewNotification("Starting workspace migration at " + beginning);
        try{
            loadProjectsList();
        } catch (RvdProjectsMigrationException e) {
            migrationSucceeded = false;
            errorCode = e.getErrorCode();
            try {
                storeMigrationStatus();
            } catch (Exception x) {
                logger.error("Error while storing workspace status", x);
            }
            throw e;
        }
        for (String projectName : projectNames) {
            try {
                // Rename Project
                String projectSid = migrateNamingConvention(projectName);
                storeLogMessage("Project '" + projectName + "' renamed to '" + projectSid + "'");

                // Generate Application entity
                generateApplicationEntity(projectSid, projectName);
                storeLogMessage("Project '" + projectName + "' synchronized with Application '"
                        + migrationHelper.getApplicationSidByProjectSid(projectSid) + "'");

                // Update IncomingPhoneNumbers
                updateIncomingPhoneNumbers(projectSid, projectName);
                storeLogMessage("IncomingPhoneNumbers updated with Application '"
                        + migrationHelper.getApplicationSidByProjectSid(projectSid) + "'");

                // Update Clients
                updateClients(projectSid, projectName);
                storeLogMessage("Clients updated with Application '"
                        + migrationHelper.getApplicationSidByProjectSid(projectSid) + "'");
            } catch (RvdProjectsMigrationException e) {
                logger.error("Error while migrating project " + projectName, e);
                migrationSucceeded = false;
                if (errorCode == null) { // Keep the first error only
                    errorCode = e.getErrorCode();
                }
                storeLogMessage(e.getMessage());
            }
            storeLogMessage("\n" + separator);
        }
        try {
            storeMigrationStatus();
        } catch (Exception e) {
            logger.error("Error while storing workspace status", e);
        }
    }

    private void loadProjectsList() throws Exception {
        this.projectNames = migrationHelper.listProjects();
    }

    private String migrateNamingConvention(String projectName) throws RvdProjectsMigrationException {
        if (!migrationHelper.projectUsesNewNamingConvention(projectName)) {
            // Change to new name standard
            String projectSid = migrationHelper.renameProjectUsingNewConvention(projectName);
            return projectSid;
        } else {
            // Once using new name standard, load project state to proceed with migration
            migrationHelper.loadProjectState(projectName);
            return projectName;
        }
    }

    private void generateApplicationEntity(String projectSid, String projectName) throws RvdProjectsMigrationException {
        migrationHelper.createOrUpdateApplicationEntity(projectSid, projectName);
    }

    private void updateIncomingPhoneNumbers(String projectSid, String projectName) throws RvdProjectsMigrationException {
        migrationHelper.updateIncomingPhoneNumbers(projectSid, projectName);
    }

    private void updateClients(String projectSid, String projectName) throws RvdProjectsMigrationException {
        migrationHelper.updateClients(projectSid, projectName);
    }

    private void storeMigrationStatus() throws RvdProjectsMigrationException, URISyntaxException {
        migrationHelper.storeWorkspaceStatus(migrationSucceeded);
        String end = getTimeStamp();
        if (!migrationSucceeded) {
            String message = "Workspace migration finished with errors at ";
            logger.error(message + end);
            storeLogMessage(message + end + "\n" + separator);
            storeNewNotification(message + end);
        } else {
            String message = "Workspace migration finished with success at " + end;
            logger.info(message + end);
            storeLogMessage(message + end + "\n" + separator);
            storeNewNotification(message + end);
        }
    }

    private void storeLogMessage(String message) throws RvdProjectsMigrationException {
        try {
            String pathName = logPath + "workspace-migration.log";
            File file = new File(pathName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file, true);
            fw.write(message);
            fw.close();
        } catch (Exception e) {
            throw new RvdProjectsMigrationException(
                    "[ERROR-CODE:10] Error while writing to file RESTCOMM_HOME/workspace-migration.log");
        }
        logger.error("Error while writing to file RESTCOMM_HOME/workspace-migration.log");
    }

    private String getTimeStamp() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeZone tz = DateTimeZone.getDefault();
        return new Timestamp(date.toDateTime(tz).toDateTime(DateTimeZone.UTC).getMillis()).toString();
    }

    private void storeNewNotification(String message) throws URISyntaxException {
        migrationHelper.addNotification(message, errorCode);
    }


}
