/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.rvd.storage;

import org.mobicents.servlet.restcomm.rvd.model.WorkspaceSettings;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageEntityNotFound;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

/**
 * @author Orestis Tsakiridis
 */
public class FsWorkspaceSettingsDao implements WorkspaceSettingsDao {
    private WorkspaceStorage workspaceStorage;

    public FsWorkspaceSettingsDao(WorkspaceStorage workspaceStorage) {
        this.workspaceStorage = workspaceStorage;
    }

    @Override
    public WorkspaceSettings loadWorkspaceSettings() {
        WorkspaceSettings workspaceSettings;
        try {
            workspaceSettings = workspaceStorage.loadEntity(".settings", "", WorkspaceSettings.class);
        } catch (StorageEntityNotFound e) {
            workspaceSettings = null;
        } catch (StorageException e) {
            throw new RuntimeException("Error loading workspace .settings from '" + workspaceStorage.rootPath + "'" ,e);
        }
        return workspaceSettings;
    }

    @Override
    public void saveWorkspaceSettings(WorkspaceSettings workspaceSettings) {
        try {
            workspaceStorage.storeEntity(workspaceSettings, WorkspaceSettings.class, ".settings", "");
        } catch (StorageException e) {
            throw new RuntimeException("Error saving workspace .settings in '" + workspaceStorage.rootPath + "'", e);
        }
    }
}
