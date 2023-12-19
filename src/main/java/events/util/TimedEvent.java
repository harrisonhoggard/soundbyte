package events.util;

import java.util.HashMap;
import java.util.Map;

// The abstract class for timed events, which are executed based on the execute time stored, instead of as a reaction to a Discord event.
public abstract class TimedEvent extends EventObject{

    public static Map<String, TimedEvent> timedEvents;

    public abstract int getExecuteTime();

    public abstract void execute();

    public static void initTimedEvents() {
        timedEvents = new HashMap<>();

        // Any timed event you want to make (birthday reminders, holidays, etc.) need to be added to the map to be executed:
    }
}
