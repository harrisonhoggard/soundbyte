package events;

import events.util.EventObject;
import bot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

// If a guild later decides to change their mind and delete the dev-channel, the preference is updated for that guild.
public class DeleteDevChannel extends EventObject {

    private Guild guild;

    @Override
    public String getName() {
        return "DeleteDevChannel";
    }

    @Override
    public String getAction() {
        return "guild deleted dev channel";
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    public void onChannelDelete(ChannelDeleteEvent event) {
        if (event.getChannel().getName().compareTo("dev-channel") != 0)
            return;

        guild = event.getGuild();
        Bot.aws.updateTableItem("SoundByteServerList", "ServerID", guild.getId(), "Dev Channel Preference", "false");

        devMessage(getName(), getAction(), getGuild());
    }
}
