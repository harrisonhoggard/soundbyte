package events.util;

import java.util.HashMap;
import java.util.Map;

public abstract class TimedEvent extends EventObject{

    public static Map<String, TimedEvent> timedEvents;

    public abstract int getExecuteTime();

    public abstract void execute();

    public static void initTimedEvents() {
        timedEvents = new HashMap<>();
    }
}
