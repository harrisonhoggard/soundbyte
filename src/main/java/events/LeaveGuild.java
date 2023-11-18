package events;

import audio.PlayerManager;
import events.util.EventObject;
import bot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;

// Handles the deleting of stored information if a bot is removed from a guild.
public class LeaveGuild extends EventObject {
    private Guild guild;

    @Override
    public String getName() {
        return "LeaveGuild";
    }

    @Override
    public String getAction() {
        return "left guild";
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    public void onGuildLeave(GuildLeaveEvent event) {
        guild = event.getGuild();

        Bot.aws.deleteTableItem("SoundByteServerList", "ServerID", event.getGuild().getId());
        Bot.aws.deleteTable(event.getGuild().getId() + "-ultrarare");
        Bot.aws.deleteBucket(event.getGuild().getId() + "-joinsounds");

        PlayerManager.getInstance().removeGuildManager(guild);

        devMessage(getName(), getAction(), getGuild());
    }
}
