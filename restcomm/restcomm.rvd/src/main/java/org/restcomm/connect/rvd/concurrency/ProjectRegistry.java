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

package org.restcomm.connect.rvd.concurrency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that holds memory resident data for projects like semaphores for cynchronization etc.
 * All such data are lazy by definition and should be re-initialized if missing (or dumped).
 *
 * MAKE SURE THIS STRUCTURE REMAINS THIN!
 *
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class ProjectRegistry {
    private Map<String, ResidentProjectInfo> projects = new ConcurrentHashMap<String, ResidentProjectInfo>();

    // Retrieves semaphore for a specific project. In case they do not exists yet they are created.
    public ResidentProjectInfo getProjectSemaphores(String applicationId) {
        ResidentProjectInfo residentProjectInfo = projects.get(applicationId);
        if (residentProjectInfo == null) {
            // the synchronized operation is rare. Only once per project per application launch
            synchronized (projects) {
                // retrieve again in case things have changed
                residentProjectInfo = projects.get(applicationId);
                if (residentProjectInfo == null) {
                    residentProjectInfo = new ResidentProjectInfo();
                    projects.put(applicationId, residentProjectInfo);
                }
            }
        }
        return residentProjectInfo;
    }

}
