package bot;

import java.io.*;

// Customized logger that prints developer messages and Discord events to the console.
// A string builder is used to assemble the printed messages, and at the end of each day, or when program closes, it prints the string to a log file. 
// Log files get uploaded to the Logger Bucket in S3.
public class Logger {
    private final StringBuilder sb;
    private File file;

    // The name that is printed in the logger.
    private static String getLogType() {
        return "LOGGER";
    }

    // Initializes the string builder and first log file.
    public Logger() {
        this.sb = new StringBuilder();
        log(getLogType(), "Logger initialized");
        this.file = new File("log/" + Bot.getDate() + ".log");
    }

    // Adds the current message to the string builder variable.
    private void appendMsg(String msg) {
        sb.append(msg).append("\n");
    }

    // Displays log message, and calls the append method.
    public void log(String type, String msg) {
        String logMsg = ("[" + type + "] " + Bot.getDateTime() + " " + msg);
        System.out.println(logMsg);
        appendMsg(logMsg);
    }

    // Takes the current string builder, appends it to the day's log, and creates the new day's log, resetting the string builder.
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void writeToFile() {
        log(getLogType(), "Writing to file");

        File dir = new File("log");

        if (!(dir.exists()))
        {
            log(getLogType(), "Making log directory at " + dir.getAbsolutePath());
            //noinspection ResultOfMethodCallIgnored
            dir.mkdir();
        }

        if (!Bot.aws.downloadObject(Config.get("BOT_NAME") + "-logs", file.getName(), file.getPath()))
        {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
                log(getLogType(), "Created " + file.getName() + " file at " + file.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;

        try {
            fw = new FileWriter(file, true);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            pw.println(sb.toString());
            pw.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                assert pw != null;
                pw.close();
                bw.close();
                fw.close();
            } catch (IOException e) {
                log(getLogType(), "IOException");
            }
        }

        String bucketName = Config.get("BOT_NAME") + "-logs";

        if (!Bot.aws.verifyObject(bucketName, file.getName()))
            Bot.aws.deleteObject(bucketName, file.getName());

        Bot.aws.uploadObject(bucketName, file.getName(), file.getPath());

        sb.delete(0, sb.length());

        this.file.delete();

        log(getLogType(), "Deleted local save of " + file.getName());

        this.file = new File("log/" + Bot.getDate() + ".log");
    }
}
