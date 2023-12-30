package events;

import bot.Bot;
import events.util.EventObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

import java.awt.*;

// Welcomes new members to a server.
public class MemberJoinedGuild extends EventObject {
    private Guild guild;

    @Override
    public String getName() {
        return "MemberJoinedGuild";
    }

    @Override
    public String getAction() {
        return "joined the guild; now at " + guild.getMembers().size() + " members.";
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        guild = event.getGuild();

        Bot.defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                    .setColor(Color.cyan)
                    .addField("New Member", "Hello " + event.getMember().getAsMention() + ", and welcome to " + guild.getName(), false)
                    .build())
                .queue();

        devMessage(getName(), event.getMember().getEffectiveName() + " " + getAction(), getGuild());
    }
}
