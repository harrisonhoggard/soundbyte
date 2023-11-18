package events;

import events.util.EventObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

import java.awt.*;
import java.util.Objects;

// Welcomes new users to servers.
public class MemberJoinedGuild extends EventObject {
    private Guild guild;

    @Override
    public String getName() {
        return "MemberJoinedGuild";
    }

    @Override
    public String getAction() {
        return "joined the guild";
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        guild = event.getGuild();

        Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setColor(Color.cyan)
                    .addField("New Member", "Hello " + event.getMember().getAsMention() + ", and welcome to " + guild.getName(), false)
                    .build())
                .queue();

        devMessage(getName(), event.getMember().getEffectiveName() + " " + getAction(), getGuild());
    }
}
