package commands;

import commands.util.CommandObject;
import bot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Disconnects bot from the voice channel it is currently connected.
public class Leave extends CommandObject {

    private String channelName;

    public String getName() {
        return "leave";
    }

    public String extraDetails() {
        return "bot left channel \"" + channelName + "\"";
    }

    public List<String> getArgs() {
        return new ArrayList<>();
    }

    public List<String> getArgInfo() {
        return new ArrayList<>();
    }

    public String getType() {
        return "Basic";
    }

    public String getDesc() {
        return "disconnects bot from current voice channel";
    }

    public boolean getAdmin() {
        return false;
    }

    public boolean getOwner() {
        return false;
    }

    // Sets channel name variable to use in the dev message, then executes the leaveVC method in the Bot class.
    public void execute(Guild guild, Member member, MessageChannel channel, String[] arg, List<Message.Attachment> attachments) {
        try {
            channelName = Objects.requireNonNull(guild.getAudioManager().getConnectedChannel()).getName();
        } catch (NullPointerException e) {
            channelName = "null";
        }

        Bot.leaveVc(guild, channel);
    }
}
