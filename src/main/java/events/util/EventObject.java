package events.util;

import events.*;
import bot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

// The blueprint for events. Their initialization is contained here.
public abstract class EventObject extends ListenerAdapter {

    // Returns certain details for each event
    public abstract String getName();
    public abstract String getAction();
    public abstract Guild getGuild();

    protected static String getLogType() {
        return "EVENT";
    }

    public static void init() {
        // Any events you want to add need to get added to the event listener here:
        Bot.jda.addEventListener(new MessageReact());
        Bot.jda.addEventListener(new UserTyping());
        Bot.jda.addEventListener(new JoinedNewGuild());
        Bot.jda.addEventListener(new LeaveGuild());
        Bot.jda.addEventListener(new MemberJoinedGuild());
        Bot.jda.addEventListener(new MemberLeaveGuild());
        Bot.jda.addEventListener(new VoiceStateUpdate());

        TimedEvent.initTimedEvents();

        Bot.log(getLogType(), "Events successfully initialized");
    }

    public void devMessage(String name, String action, Guild guild) {
        Bot.log(getLogType(), guild.getName() + ": executed " + name + ": " + action + ";");
    }
}
