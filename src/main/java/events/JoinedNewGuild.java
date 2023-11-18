package events;

import events.util.EventObject;
import bot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

// Responsible for initializing a new guild that a bot joined.
public class JoinedNewGuild extends EventObject {
    private Guild guild;

    @Override
    public String getName() {
        return "JoinedNewGuild";
    }

    @Override
    public String getAction() {
        return "joined a new guild";
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    public void onGuildJoin(GuildJoinEvent event) {
        guild = event.getGuild();

        Bot.guildInit(event.getGuild());

        devMessage(getName(), getAction(), getGuild());
    }
}
