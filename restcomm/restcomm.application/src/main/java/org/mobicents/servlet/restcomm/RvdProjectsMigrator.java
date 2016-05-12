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
 * The goal of this class is to generate an Application entity inside the database for each RVD project located inside its
 * workspace. Also, apply the new naming convention on project directories inside the workspace, based on a new
 * {@link org.mobicents.servlet.restcomm.entities.Sid.Type.PROJECT} generated to each entry.
 *
 * @author guilherme.jansen@telestax.com
 */
public class RvdProjectsMigrator {

    private static final Logger logger = Logger.getLogger(RvdProjectsMigrator.class);
    private static final String separator = "--------------------------------------";
    private RvdProjectsMigrationHelper migrationHelper;
    private List<String> projectNames;
    private boolean migrationSucceeded;
    private Integer errorCode;
    private String logPath;

    private int projectsProcessed;
    private int projectsSuccess;
    private int projectsError;
    private int updatedDids;
    private int updatedClients;

    public RvdProjectsMigrator(ServletContext servletContext, Configuration configuration) throws Exception {
        this.migrationHelper = new RvdProjectsMigrationHelper(servletContext, configuration);
        this.migrationSucceeded = true;
        this.logPath = servletContext.getRealPath("/") + "../../../"; // Equivalent to RESTCOMM_HOME
        this.errorCode = 0;
        this.projectsProcessed = 0;
        this.projectsSuccess = 0;
        this.projectsError = 0;
        this.updatedDids = 0;
        this.updatedClients = 0;
    }

    public void executeMigration() throws Exception {
        String beginning = getTimeStamp();
        // Ensure the migration needs to be executed
        if (!migrationHelper.isMigrationEnabled() || migrationHelper.isMigrationExecuted()) {
            storeNewMessage("Workspace migration skipped in " + beginning, true, true, true, false);
            storeNewMessage(separator, false, true, false, false);
            return;
        }
        storeNewMessage("Starting workspace migration at " + beginning, true, true, true, false);
        storeNewMessage(separator, false, true, false, false);
        try {
            if (!migrationHelper.isEmbeddedMigration()) {
                backupWorkspace();
            }
            loadProjectsList();
        } catch (RvdProjectsMigrationException e) {
            migrationSucceeded = false;
            errorCode = e.getErrorCode();
            storeNewMessage(e.getMessage(), true, true, false, true);
            try {
                storeMigrationStatus();
            } catch (Exception x) {
                storeNewMessage("[ERROR-CODE:2] Error while storing workspace status" + x.getMessage(), true, true, false, true);
            }
            throw e;
        }
        for (String projectName : projectNames) {
            try {
                // Load Project State Header
                migrationHelper.loadProjectState(projectName);

                // Check if this project is already synchronized with a application
                String applicationSid = searchApplicationSid(projectName);

                // Synchronize with application entity if needed
                applicationSid = synchronizeApplicationEntity(applicationSid, projectName);

                // Rename Project
                migrateNamingConvention(projectName, applicationSid);

                // Update IncomingPhoneNumbers
                updateIncomingPhoneNumbers(applicationSid, projectName);

                // Update Clients
                updateClients(applicationSid, projectName);

                projectsSuccess++;
            } catch (RvdProjectsMigrationException e) {
                migrationSucceeded = false;
                if (errorCode == 0) { // Keep the first error only
                    errorCode = e.getErrorCode();
                }
                projectsError++;
                storeNewMessage("Error while migrating project '" + projectName + "' " + e.getMessage(), false, true, false,
                        true);
            }
            projectsProcessed++;
            storeNewMessage(separator, false, true, false, false);
        }
        try {
            storeMigrationStatus();
        } catch (Exception e) {
            storeNewMessage("[ERROR-CODE:2] Error while storing workspace status " + e, true, true, false, true);
            throw e;
        }
    }

    private void loadProjectsList() throws Exception {
        this.projectNames = migrationHelper.listProjects();
    }

    private String searchApplicationSid(String projectName) {
        return migrationHelper.searchApplicationSid(projectName);
    }

    private String synchronizeApplicationEntity(String applicationSid, String projectName)
            throws RvdProjectsMigrationException, URISyntaxException {
        if (!projectName.equals(applicationSid)) {
            applicationSid = migrationHelper.createOrUpdateApplicationEntity(applicationSid, projectName);
            storeNewMessage("Project '" + projectName + "' synchronized with Application '" + applicationSid + "'", false,
                    true, false, false);
        } else {
            storeNewMessage("Project '" + projectName + "' previously synchronized with Application '" + applicationSid
                    + "'. Skipped", false, true, false, false);
        }
        return applicationSid;
    }

    private void migrateNamingConvention(String projectName, String applicationSid) throws RvdProjectsMigrationException,
            URISyntaxException {
        if (!projectName.equals(applicationSid)) {
            migrationHelper.renameProjectUsingNewConvention(projectName, applicationSid);
            storeNewMessage("Project '" + projectName + "' renamed to '" + applicationSid + "'", false, true, false, false);
        } else {
            storeNewMessage("Project " + projectName + " already using new naming convention. Skipped", false, true, false,
                    false);
        }
    }


    private void updateIncomingPhoneNumbers(String applicationSid, String projectName) throws RvdProjectsMigrationException,
            URISyntaxException {
        int amountUpdated = migrationHelper.updateIncomingPhoneNumbers(applicationSid, projectName);
        if (amountUpdated > 0) {
            storeNewMessage("Updated " + amountUpdated + " IncomingPhoneNumbers with Application '" + applicationSid + "'",
                    false, true, false, false);
            updatedDids += amountUpdated;
        } else {
            storeNewMessage("No IncomingPhoneNumbers found to update with Application '" + applicationSid + "'. Skipped",
                    false, true, false, false);
        }
    }

    private void updateClients(String applicationSid, String projectName) throws RvdProjectsMigrationException, URISyntaxException {
        int amountUpdated = migrationHelper.updateClients(applicationSid, projectName);
        if (amountUpdated > 0) {
            storeNewMessage("Updated " + amountUpdated + " Clients with Application '" + applicationSid + "'", false, true,
                    false, false);
            updatedClients += amountUpdated;
        } else {
            storeNewMessage("No Clients found to update with Application '" + applicationSid + "'. Skipped", false, true,
                    false, false);
        }
    }

    private void storeMigrationStatus() throws RvdProjectsMigrationException, URISyntaxException {
        migrationHelper.storeWorkspaceStatus(migrationSucceeded);
        String end = getTimeStamp();
        if (!migrationSucceeded) {
            String message = "Workspace migration finished with errors at " + end;
            message += ". Status: " + projectsProcessed + " Projects processed (";
            message += projectsSuccess + " with success and " + projectsError + " with error), ";
            message += updatedDids + " IncomingPhoneNumbers and " + updatedClients + " Clients updated";
            storeNewMessage(message, true, true, true, true);
            storeNewMessage(separator, false, true, false, false);
            sendEmailNotification(message);
        } else {
            String message = "Workspace migration finished with success at " + end;
            message += ". Status: " + projectsProcessed + " Projects processed (";
            message += projectsSuccess + " with success and " + projectsError + " with error), ";
            message += updatedDids + " IncomingPhoneNumbers and " + updatedClients + " Clients updated";
            storeNewMessage(message, true, true, true, false);
            storeNewMessage(separator, false, true, false, false);
            sendEmailNotification(message);
        }
    }

    private void storeNewMessage(String message, boolean asServerLog, boolean asMigrationLog, boolean asNotification,
            boolean error) throws RvdProjectsMigrationException, URISyntaxException {
        // Write to server log
        if (asServerLog) {
            if (error) {
                logger.error(message);
            } else if(logger.isInfoEnabled()) {
                logger.info(message);
            }
        }
        // Write to migration log, but use server log if embedded migration
        if (asMigrationLog) {
            if (!migrationHelper.isEmbeddedMigration()) {
                storeLogMessage(message);
            } else if (!asServerLog) { // Prevent duplicated messages
                if (error) {
                    logger.error(message);
                } else  if(logger.isInfoEnabled()) {
                    logger.info(message);
                }
            }
        }
        // Create new notification
        if (asNotification) {
            storeNewNotification(message);
        }
    }

    private void storeLogMessage(String message) throws RvdProjectsMigrationException, URISyntaxException {
        try {
            String pathName = logPath + "workspace-migration.log";
            File file = new File(pathName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file, true);
            fw.write(message + "\n");
            fw.close();
        } catch (Exception e) {
            storeNewMessage("[ERROR-CODE:3] Error while writing to file RESTCOMM_HOME/workspace-migration.log", true, false,
                    false, true);
        }
    }

    private String getTimeStamp() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeZone tz = DateTimeZone.getDefault();
        return new Timestamp(date.toDateTime(tz).toDateTime(DateTimeZone.UTC).getMillis()).toString();
    }

    private void storeNewNotification(String message) throws URISyntaxException {
        migrationHelper.addNotification(message, migrationSucceeded, new Integer(errorCode));
    }

    private void sendEmailNotification(String message) throws RvdProjectsMigrationException, URISyntaxException {
        try {
            migrationHelper.sendEmailNotification(message, migrationSucceeded);
        } catch (RvdProjectsMigrationException e) {
            storeNewMessage("[ERROR-CODE:4] Workspace migration email notification skipped due to invalid configuration", true,
                    true, false,
                    true);
            storeNewMessage(separator, false, true, false, false);
        }
    }

    private void backupWorkspace() throws RvdProjectsMigrationException {
        migrationHelper.backupWorkspace();
    }

}
