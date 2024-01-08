package events;

import events.util.EventObject;
import bot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// Removes any bucket objects and/or table entries associated with the member that left a server.
public class MemberLeaveGuild extends EventObject {

    private Guild guild;

    @Override
    public String getName() {
        return "MemberLeaveGuild";
    }

    @Override
    public String getAction() {
        return "left the guild; now at " + guild.getMembers().size() + " members.";
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        if (event.getUser().isBot())
            return;

        guild = event.getGuild();

        if (Bot.aws.verifyObject(guild.getId() + "-joinsounds", Objects.requireNonNull(event.getMember()).getId() + ".ogg"))
            Bot.aws.deleteObject(guild.getId() + "-joinsounds", event.getMember().getId() + ".ogg");

        Bot.aws.deleteTableItem(guild.getId() + "-ultrarare", "MemberID", event.getMember().getId());

        devMessage(getName(), event.getMember().getEffectiveName() + " " + getAction(), getGuild());
    }
}
