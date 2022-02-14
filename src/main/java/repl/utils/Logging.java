package repl.utils;

import org.apache.log4j.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * omf.Logging! Will always write to the console, but can specify to also write to a
 * file.
 * <p/>
 * In classes without main methods, use getLogger(). The classes with a main
 * method will set up file logging with the other omf.Logging methods if they so
 * desire.
 *
 * @author Garth Bosch
 */
public class Logging {
    private static final String DEFAULT_OUTPUT_DIR = "reports/logs/";
    private static final String CONVERSION_PATTERN = "(%d{HH:mm:ss,SSS}) %C{1} : %-5p - %m%n";
    private static boolean rootSet = false;
    private static String tcID = "";
    private static String callingClass;
    private static final int MAX_FILENAME_LEN = 255;
    private static final String TOP_PACKAGE_NAME = "gfb.logging";

    /**
     * Get the class that is calling the logging method. This will only be
     * gotten once. The first class that calls the attempts to create the logger
     * should set up logging to a file.
     */
    static {
        callingClass = Thread.currentThread().getStackTrace()[2].getClassName();
    }

    /**
     * Change the logging level from the default value of INFO. This will affect
     * all the loggers that get initialized after changing the level.
     * <p>
     * Should be run before initializing the logger you whose level needs
     * changed.
     *
     * @param level
     */
    public static void setLogLevel(Level level) {
        Logger.getLogger(TOP_PACKAGE_NAME).setLevel(level);
    }

    /**
     * omf.Logging without writing to a file
     * <p>
     * Note: Use this when setting up logging for a class without a main method.
     *
     * @return The log4j Logger to use
     */
    public static Logger getLogger() {
        if (!rootSet) {
            setRootLogger();
            rootSet = true;
        }
        return Logger.getLogger(Thread.currentThread().getStackTrace()[2]
                .getClassName());
    }

    /**
     * ------------ Get loggers with test case IDs in the filename ---------
     */

    /**
     * Get a logger with the test caseID in the file name. It will use the
     * default directory to store the logs.
     *
     * @param testCaseID
     * @return Logger
     */
    public static Logger getLoggerTC(String testCaseID) {
        if (testCaseID != null && !(testCaseID.isEmpty())) {
            tcID = testCaseID;
        }
        return getLogger(true);
    }

    /**
     * Get a logger with the test caseID in the file name. Can specify the
     * directory where the logs will be saved.
     */
    public static Logger getLoggerTC(String testCaseID, String fDir) {
        if (testCaseID != null && !(testCaseID.isEmpty())) {
            tcID = testCaseID;
        }
        return getLogger("", fDir);
    }

    /**
     * -------- Get loggers without test case IDs ---
     */

    /**
     * omf.Logging with the option to write to a file.
     * <p>
     * -Uses default filename and output path for empty strings
     *
     * @param writeFile - Write to file?
     * @return The log4j Logger to use
     */
    public static Logger getLogger(boolean writeFile) {
        if (!rootSet) {
            if (writeFile) {
                String defaultFileName = getFileNameFromClass(callingClass);
                String relativeDir = getDirFromClass(callingClass)
                        + getDirFromDate();
                setRootLogger(defaultFileName, DEFAULT_OUTPUT_DIR + relativeDir);
            } else {
                setRootLogger();
            }
            rootSet = true;
        }
        return Logger.getLogger(callingClass);
    }

    /**
     * omf.Logging that write to file. -Specify the filename -Uses default output
     * dir
     *
     * @param fName - Name of file
     * @return The log4j Logger to use
     */
    public static Logger getLogger(String fName) {
        if (!rootSet) {
            String relativeDir = getDirFromClass(callingClass)
                    + getDirFromDate();
            if (fName == null || fName.length() <= 0) {
                fName = getFileNameFromClass(callingClass);
            }
            setRootLogger(fName, DEFAULT_OUTPUT_DIR + relativeDir);
            rootSet = true;
        }
        return Logger.getLogger(callingClass);
    }

    /**
     * omf.Logging that writes to a file. -Specify file name and output directory
     *
     * @param fName - Name of File
     * @param fDir  - Name of output dir, relative to project space
     * @return The log4j Logger to use
     */
    public static Logger getLogger(String fName, String fDir) {
        if (!rootSet) {
            if (fName == null || fName.length() <= 0) {
                fName = getFileNameFromClass(callingClass);
            }
            if (fDir == null) {
                fDir = getDirFromClass(callingClass) + getDirFromDate();
            }
            if (fDir.charAt(fDir.length() - 1) != '/') {
                fDir = fDir + "/";
            }
            setRootLogger(fName, fDir);
            rootSet = true;
        }
        return Logger.getLogger(callingClass);
    }

    /*Returns the date in this format: 2014-10-31_10-25-59*/
    public String generateDateTimeString() {
        Date dateNow = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
        return dateFormat.format(dateNow);
    }

    /**
     * Set root logger information for writing to a file -Still writes to
     * console
     *
     * @param fName
     * @param fDir
     */
    private static void setRootLogger(String fName, String fDir) {
        setRootLogger();

        try {
            // Check if directory exists
            File dir = new File(fDir);
            if (!(dir.exists())) {
                dir.mkdirs();
            }
            Logger.getRootLogger().addAppender(
                    new FileAppender(new PatternLayout(CONVERSION_PATTERN),
                            fDir + fName, false));
        } catch (IOException e) {
            e.getMessage();
        }
    }

    /**
     * Set root logger information for writing solely to console
     */
    private static void setRootLogger() {
        PatternLayout pattern = new PatternLayout();
        pattern.setConversionPattern(CONVERSION_PATTERN);

        Logger root = Logger.getRootLogger();
        root.removeAllAppenders();
        root.addAppender(new ConsoleAppender(pattern));
        root.setLevel(Level.INFO);

    }

    /**
     * Get the name of the output file from the calling class and current time
     * Ex: gfb.logging.Tester returns: Tester_[HHmm]
     * <p>
     * If a test case ID is set, it returns the test case ID in the file name
     * Ex: The above package would return: Tester_[tcID]_[HHmm]
     *
     * @param callingClass
     * @return
     */
    private static String getFileNameFromClass(String callingClass) {
        String currTime = (new SimpleDateFormat("HHmmss")).format(new Date());

        int start = callingClass.lastIndexOf('.') + 1;
        String output = (start > 0) ? callingClass.substring(start)
                : callingClass;

        // ClassName_tID_Time.log
        // or
        // ClassName_Time.log
        String fileName = output + "_";
        if (!(tcID.isEmpty())) {
            String id = tcID;
            // Check the file name length
            // Get length of class name & tcID
            int len = fileName.length() + id.length();
            // _ + HHmmss + . + log
            len += 1 + 6 + 1 + 3;
            if (len > MAX_FILENAME_LEN) {
                int numRemove = len - MAX_FILENAME_LEN;
                id = id.substring(0, id.length() - numRemove);
            }

            fileName += id + "_";
        }
        fileName += currTime + ".log";
        return fileName;
    }

    /**
     * Get the name of the directory we should put our output in from the
     * calling class package. If the last part of the package is 'test' get the
     * second to last part of the package.
     * <p>
     * Ex: gfb.logging.Tester should return ssh/
     * gfb.logging.Tester should return gfb.logging/
     *
     * @param callingClass
     * @return
     */
    private static String getDirFromClass(String callingClass) {
        int end = callingClass.lastIndexOf('.');

        if (end > 0) {
            String packName = callingClass.substring(0, end);
            end = packName.lastIndexOf('.');
            String topName = packName.substring(end + 1);
            if (topName.equalsIgnoreCase("test")) {
                int start = packName.substring(0, end).lastIndexOf('.') + 1;
                return packName.substring(start) + "/";
            } else {
                return packName.substring(end + 1) + "/";
            }
        } else {
            return callingClass + "/";
        }
    }

    /**
     * Get the name of the directory based on the current date
     *
     * @return
     */
    private static String getDirFromDate() {
        return (new SimpleDateFormat("yyyy_MM_dd").format(new Date())) + "/";
    }
}

