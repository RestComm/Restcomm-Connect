package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;

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
    private String logFilePath;
    private ModelMarshaler marshaler;
    private boolean useMarshaler;

    // Temporary variable holding log message as it is built.

    public ProjectLogger(String projectName, RvdConfiguration settings,ModelMarshaler marshaler) {
        this.projectName = projectName;
        this.logFilePath = settings.getProjectBasePath(projectName) + File.separator + RvdConfiguration.PROJECT_LOG_FILENAME;
        this.marshaler = marshaler;
        this.useMarshaler = true;
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
            FileUtils.writeStringToFile(new File(logFilePath), buffer.toString(), Charset.forName("UTF-8"), true);
        } catch (IOException e) {
            logger.warn("Error writing to application log for " + projectName, e);
        }
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    // clear the log file
    public void reset() {
        try {
            FileUtils.writeStringToFile(new File(logFilePath), "");
        } catch (IOException e) {
            logger.warn("Error clearing application log for " + projectName, e);
        }
    }


}
