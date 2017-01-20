package org.restcomm.connect.rvd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.concurrency.ResidentProjectInfo;
import org.restcomm.connect.rvd.model.ModelMarshaler;

/**
 * A logger service for an RVD project. It is supposed to help the designer of an application for easy testing debugging without the need
 * to ssh on the server and scan through log files. Each project/app has its own application log.
 * @author "Tsakiridis Orestis"
 *
 */
public class ProjectLogger {
    static final Logger logger = Logger.getLogger(ProjectLogger.class.getName());

    private static final int MAX_TAGS = 5;
    private String projectName;
    private ModelMarshaler marshaler;
    private boolean useMarshaler;

    static int backlogCount = 3; // number rotated log files in addition to the main log file (total files = backlogSize + 1)
    static int triggerRotationSize = 1000; // the size of the main log file in bytes that will trigger the rotation when exceeded
    static String logFilenameBase; // the full path of the main log file without the extension e.g. /home/user/workspace/APxxx/project (without '.log')
    File mainLogFile;

    ResidentProjectInfo semaphores;

    // Temporary variable holding log message as it is built.

    public ProjectLogger(String projectName, RvdConfiguration settings,ModelMarshaler marshaler, ResidentProjectInfo semaphores) {
        this.projectName = projectName;
        this.logFilenameBase = settings.getProjectBasePath(projectName) + File.separator + RvdConfiguration.PROJECT_LOG_FILENAME;
        mainLogFile = new File(logFilenameBase + ".log");
        this.marshaler = marshaler;
        this.useMarshaler = true;
        this.semaphores = semaphores;
    }

    private Object payload;
    private String[] tags;
    private int tagCount = 0;

    /**
     * Set the payload to be persisted on file. By using this method, marshaler will be implicitly
     * applied to payload before write to file. To skip use of marshaler, use {@link #log(Object, boolean)}
     * informing parameter <b>useMarshaler</b> as <b>false</b>.
     * @param payload
     * @return The current instance of {@link ProjectLogger}.
     */
    public ProjectLogger log(Object payload) {
        tags = new String[MAX_TAGS];
        tagCount = 0;
        this.payload = payload;
        return this;
    }

    /**
     * Allow to log skipping marshaler before write to file. This method overloads
     * {@link #log(Object)} that assumes <b>true</b> as default to the global variable
     * <b>useMarshaler</b>. To skip marshaler by using this method, the value of the parameter
     * <b>useMarshaler</b> must be informed as <b>false</b>.
     * @param payload
     * @param useMarshaler
     * @return The current instance of {@link ProjectLogger}.
     */
    public ProjectLogger log(Object payload, boolean useMarshaler){
        this.useMarshaler = useMarshaler;
        return log(payload);
    }

    public ProjectLogger tag(String name, String value) {
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

    public ProjectLogger tag(String name) {
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
        if(useMarshaler){
            buffer.append(marshaler.toData(payload));
        } else {
            buffer.append(String.valueOf(payload));
        }
        buffer.append(System.getProperty("line.separator"));  //add a newline
        // data is ready for writing. Make sure no newlines are there

        try {
            FileUtils.writeStringToFile(mainLogFile, buffer.toString(), Charset.forName("UTF-8"), true);
        } catch (IOException e) {
            logger.warn("Error writing to application log for " + projectName, e);
        }

        // check for log retation
        if (needsRotate())
            rotate();
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
            logger.warn("Error clearing application log for " + projectName, e);
        }
    }

    // do we need to rotate the application log files ?
    boolean needsRotate() {
        if ( mainLogFile.length() >  triggerRotationSize) {
            return true;
        }
        return false;
    }

    /*
    rename project-n-1.log -> project-n.log
    rename project-n-2.log -> project-n-1.log
    copy project.log -> project1.log
    create project-new.log
    rename project-new.log -> project.log
    */
    void rotate() {
        /*  Rotation algorithm
            create project-new.log (should atomic on FS level and fail if it already exists)
            rename project-n-1.log -> project-n.log
            rename project-n-2.log -> project-n-1.log
            copy project.log -> project1.log
            rename project-new.log -> project.log
        */
        synchronized (semaphores.logRotationSemaphore) {
            // create a new blank file (it will become the new mainlog file)
            // TODO make sure file creation fails if the file is already there.
            // TODO abort rotation in case another rotation is already in place i.e. project-new.log already exists
            File newfile = new File(logFilenameBase + "-new.log");
            // increase index of all backlog files (rename)
            for (int i = backlogCount - 1; i <= 1; i--) {
                File backlogFile = new File(logFilenameBase + "-" + i + ".log");
                backlogFile.renameTo(new File(logFilenameBase + "-" + (i + 1) + ".log"));
            }
            // copy main log file to the backlog
            try {
                FileUtils.copyFile(mainLogFile, new File(logFilenameBase + "-1.log"));
            } catch (IOException e) {
                throw new RuntimeException("Error rotating application log files for project " + projectName, e);
            }
            // rename the new blank file to the name of the main log file
            newfile.renameTo(mainLogFile);
        }
    }




}
