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

package org.restcomm.connect.rvd.logging;

import java.io.File;

import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.concurrency.LogRotationSemaphore;
import org.restcomm.connect.rvd.model.ModelMarshaler;

/**
 * A logger service for an RVD project. It is supposed to help the designer of an application for easy testing debugging without the need
 * to ssh on the server and scan through log files. Each project/app has its own application log.
 * @author "Tsakiridis Orestis"
 *
 */
public class ProjectLogger extends CustomLogger {

    private ModelMarshaler marshaler;
    private boolean useMarshaler;

    public ProjectLogger(String projectName, RvdConfiguration settings, ModelMarshaler marshaler, LogRotationSemaphore semaphore) {
        this(settings.getProjectBasePath(projectName) + File.separator + RvdConfiguration.PROJECT_LOG_FILENAME, marshaler, semaphore);
    }

    public ProjectLogger(String logFilenameBase, ModelMarshaler marshaler, LogRotationSemaphore semaphore) {
        super(logFilenameBase, semaphore);
        this.marshaler = marshaler;
        this.useMarshaler = true;
    }

    /**
     * Set message/payload for logging. This method allows skipping marshalling of data (marchaling enabled by default).
     * To do that, set useMarshaler to false.
     *
     * @param payload
     * @param useMarshaler
     *
     * @return The current instance of {@link ProjectLogger}.
     */
    public ProjectLogger log(Object payload, boolean useMarshaler){
        this.useMarshaler = useMarshaler;
        super.log(payload);
        return this;
    }

    public ProjectLogger log(Object payload) {
        this.useMarshaler = true; // default
        super.log(payload);
        return this;
    }

    public ProjectLogger tag(String name, String value) {
        super.tag(name, value);
        return this;
    }

    public ProjectLogger tag(String name) {
        return tag(name, null);
    }

    @Override
    protected void append(StringBuffer buffer) {
        if(useMarshaler){
            buffer.append(marshaler.toData(payload));
        } else {
            buffer.append(String.valueOf(payload));
        }
    }

}
