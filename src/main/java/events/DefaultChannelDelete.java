package events;

import bot.Bot;
import bot.Config;
import events.util.EventObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;

import java.util.Objects;

public class DefaultChannelDelete extends EventObject {

    @Override
    public String getName() {
        return "DefaultChannelDelete";
    }

    @Override
    public String getAction() {
        return "default channel was deleted";
    }

    @Override
    public Guild getGuild() {
        return null;
    }

    public void onChannelDelete(ChannelDeleteEvent event) {
        if (event.getChannel().getId().equals(Bot.defaultChannels.get(event.getGuild()).getId()))
        {
            Guild guild = event.getGuild();
            Objects.requireNonNull(guild.getOwner()).getUser().openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(new EmbedBuilder()
                    .addField("Default Channel deleted", "Hello, the text channel I was using for your server named \"" + guild.getName() + "\" was recently deleted." +
                            "\nIn order for me to function properly, I will need a default channel to send messages in, and you can set that up by typing in **\"" + Config.get("COMMAND_PREFIX") +
                            " channel\"** inside the text channel you want me to use.\n\nThank you", false).build()))
                    .queue();

            devMessage(getName(), getAction(), guild);
        }
    }
}
