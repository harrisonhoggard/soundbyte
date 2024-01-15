package bot;

// Useful to catch any exceptions I haven't caught in testing. If they occur, they'll be printed in the log file
public class UncaughtException implements Thread.UncaughtExceptionHandler {

    public UncaughtException() {
        Bot.log(getLogType(), "Started UncaughtExceptionHandler");
    }

    private String getLogType() {
        return "UNCAUGHT EXCEPTION";
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Bot.log(getLogType(), "Thread: " + t.getName() + "; Exception: " + e.toString());

        Bot.log(getLogType(), "Updating log file due to exception");
        Bot.writeLog();
    }
}
