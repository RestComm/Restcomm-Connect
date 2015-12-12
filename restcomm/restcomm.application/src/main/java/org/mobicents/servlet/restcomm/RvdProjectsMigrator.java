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

import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

/**
 * The goal of this class is to generate an Application entity inside the
 * database for each RVD project located inside its workspace.
 * Also, apply the new naming convention on project directories inside
 * the workspace, based on a new {@link org.mobicents.servlet.restcomm.entities.Sid.Type.PROJECT} generated to each entry.
 *
 * @author guilherme.jansen@telestax.com
 */
public class RvdProjectsMigrator {

    private static final Logger logger = Logger.getLogger(RvdProjectsMigrator.class);
    private RvdProjectMigrationHelper migrationHelper;
    private List<String> projectNames;
    private boolean migrationSucceeded;


    public RvdProjectsMigrator(ServletContext servletContext, Configuration configuration) throws Exception {
        this.migrationHelper = new RvdProjectMigrationHelper(servletContext, configuration);
        this.migrationSucceeded = true;
    }

    public void executeMigration() throws Exception {
        if (migrationHelper.readWorkspaceStatus()) {
            return;
        }
        logger.info("RVD Projects Migrator started...");
        try{
            loadProjectsList();
        } catch(Exception e){
            migrationSucceeded = false;
            storeMigrationStatus();
            throw e;
        }
        for (String projectName : projectNames) {
            try {
                String projectSid = migrateNamingConvention(projectName);
                generateApplicationEntity(projectSid);
                updateIncomingPhoneNumbers(projectSid);
                updateClients(projectSid);
            } catch (Exception e) {
                logger.error("Error while migrating project " + projectName, e);
                migrationSucceeded = false;
            }
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

    private String migrateNamingConvention(String projectName) throws Exception {
        if (!migrationHelper.projectUsesNewNamingConvention(projectName)) {
            // Store name inside project state
            migrationHelper.storeProjectName(projectName);
            // Change to new name standard
            String projectSid = migrationHelper.renameProjectUsingNewConvention(projectName);
            return projectSid;
        } else {
            // Once using new name standard, load project state to proceed with migration
            migrationHelper.loadProjectState(projectName);
            return projectName;
        }
    }

    private void generateApplicationEntity(String projectSid) throws Exception {
        migrationHelper.createOrUpdateApplicationEntity(projectSid);
    }

    private void updateIncomingPhoneNumbers(String projectSid) throws Exception {
        migrationHelper.updateIncomingPhoneNumbers(projectSid);
    }

    private void updateClients(String projectSid) throws Exception {
        migrationHelper.updateClients(projectSid);
    }

    private void storeMigrationStatus() throws Exception {
        migrationHelper.storeWorkspaceStatus(migrationSucceeded);
        if (!migrationSucceeded) {
            logger.error("RVD Projects Migrator finished with errors");
        } else {
            logger.info("RVD Projects Migrator finished with success");
        }
    }


}
