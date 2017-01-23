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

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.concurrency.LogRotationSemaphore;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * A logger service for an RVD project. It is supposed to help the designer of an application for easy testing debugging without the need
 * to ssh on the server and scan through log files. Each project/app has its own application log.
 * @author "Tsakiridis Orestis"
 *
 */
public class CustomLogger {
    static final Logger logger = Logger.getLogger(CustomLogger.class.getName());

    protected static final int MAX_TAGS = 5;

    int backlogCount = RvdConfiguration.PROJECT_LOG_BACKLOG_COUNT; // number rotated log files in addition to the main log file (total files = backlogSize + 1)
    int triggerRotationSize = RvdConfiguration.PROJECT_LOG_ROTATION_SIZE; // the size of the main log file in bytes that will trigger the rotation when exceeded
    String logFilenameBase; // the full path of the main log file without the extension e.g. /home/user/workspace/APxxx/project (without '.log')
    File mainLogFile;

    LogRotationSemaphore semaphore;

    protected Object payload;
    protected String[] tags;
    protected int tagCount = 0;

    public CustomLogger(String logFilenameBase, LogRotationSemaphore semaphore ) {
        this.logFilenameBase = logFilenameBase;
        mainLogFile = new File(logFilenameBase + ".log");
        this.semaphore = semaphore;
    }

    public CustomLogger(String logFilenameBase, int maxSize,  int backlogCount, LogRotationSemaphore semaphore) {
        this(logFilenameBase, semaphore);
        if (maxSize <= 0 || backlogCount <= 0)
            throw new IllegalArgumentException("Cannot initialize CustomLogger");

        this.triggerRotationSize = maxSize;
        this.backlogCount = backlogCount;
    }

    public CustomLogger log(Object payload) {
        tags = new String[MAX_TAGS];
        tagCount = 0;
        this.payload = payload;
        return this;
    }

    public CustomLogger tag(String name, String value) {
        if ( tagCount < MAX_TAGS ) {
            if ( value == null )
                tags[tagCount] = "[" + name + "]";
            else
                tags[tagCount] = "[" + name + " " + value +"]";
            tagCount ++;
        } else {
            logger.warn("ProjectLogger: Cannot add any more tags to the log entry" );
        }
        return this;
    }

    public CustomLogger tag(String name) {
        return tag(name, null);
    }

    public void done() {
        Date date = new Date();
        StringBuffer buffer = new StringBuffer();
        buffer.append("[" + date.toString() + "]");
        for ( String tag : tags ) {
            if (tag == null)
                break;
            buffer.append( tag);
        }
        if ( buffer.length() > 0 )
            buffer.append(" ");
        append(buffer);
        buffer.append(System.getProperty("line.separator"));  //add a newline
        // data is ready for writing. Make sure no newlines are there

        try {
            FileUtils.writeStringToFile(mainLogFile, buffer.toString(), Charset.forName("UTF-8"), true);
        } catch (IOException e) {
            logger.warn("Error writing to application log to " + logFilenameBase, e);
        }

        // check for log retation
        if (needsRotate())
            rotate();
    }

    protected void append(StringBuffer buffer) {
        buffer.append(String.valueOf(payload));
    }

    public String getLogFilePath() {
        return mainLogFile.getPath();
    }

    // clear the log file
    // TODO check this method for concurrency issues
    public void reset() {
        try {
            FileUtils.writeStringToFile(mainLogFile, "");
        } catch (IOException e) {
            logger.warn("Error clearing application log to " + logFilenameBase, e);
        }
    }

    // do we need to rotate the application log files ?
    boolean needsRotate() {
        long length = mainLogFile.length();
        if ( length >  triggerRotationSize) {
            return true;
        }
        return false;
    }

    void rotate() {
        /*  Rotation algorithm
            create project-new.log (should atomic on FS level and fail if it already exists)
            rename project-n-1.log -> project-n.log
            rename project-n-2.log -> project-n-1.log
            copy project.log -> project1.log
            rename project-new.log -> project.log
        */
        synchronized (semaphore) {
            try {
                // create a new blank file (it will become the new mainlog file)
                // TODO make sure file creation fails if the file is already there.
                // TODO abort rotation in case another rotation is already in place i.e. project-new.log already exists
                File newfile = new File(logFilenameBase + "-new.log");
                newfile.createNewFile();

                // increase index of all backlog files (rename)
                for (int i = backlogCount - 1; i >= 1; i--) {
                    File backlogFile = new File(logFilenameBase + "-" + i + ".log");
                    if (backlogFile.exists())
                        backlogFile.renameTo(new File(logFilenameBase + "-" + (i + 1) + ".log"));
                }
                // copy main log file to the backlog
                mainLogFile.renameTo(new File(logFilenameBase + "-1.log"));
                // rename the new blank file to the name of the main log file
                newfile.renameTo(mainLogFile);

            } catch (IOException e) {
                throw new RuntimeException("Error rotating application log files for project " + logFilenameBase, e);
            }
        }
    }




}
