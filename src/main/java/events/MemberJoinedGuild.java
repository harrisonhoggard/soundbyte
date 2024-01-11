package events;

import bot.Config;
import events.util.EventObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

// Welcomes new members to a server.
public class MemberJoinedGuild extends EventObject {
    private Guild guild;
    String details = "";

    @Override
    public String getName() {
        return "MemberJoinedGuild";
    }

    @Override
    public String getAction() {
        return "joined the guild; now at " + guild.getMembers().size() + " members; " + details;
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getUser().isBot())
            return;

        guild = event.getGuild();

        try {
            guild.modifyWelcomeScreen().getWelcomeChannels();
        } catch (InsufficientPermissionException e) {
            event.getMember().getUser().openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(new EmbedBuilder().setDescription("**Hello " + event.getUser().getEffectiveName() +
                                    ",** \n\nWelcome to " + guild.getName() + "! I am " + Config.get("BOT_NAME") +
                                    ". I play custom sounds whenever someone joins a voice channel (think small ringtones)," +
                                    " which is a great way to determine who joined without needing to look! \n\nIf you want to customize a sound, just type in **\"" +
                                    Config.get("COMMAND_PREFIX") + "\"** help to get started. \n\nTo invite me to your server, find me on Top.gg")
                        .build()))
                    .queue();
            details = "welcomed new member";
        }

        devMessage(getName(), event.getMember().getEffectiveName() + " " + getAction(), getGuild());
    }
}
