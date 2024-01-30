package commands;

import commands.util.CommandObject;
import bot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Moves the bot into either a specified voice channel, or the channel the executing user is in currently.
public class Join extends CommandObject {

    private String channelName;
    
    public String getName() {
        return "join";
    }

    public String extraDetails() {
        if (channelName == null)
            return "no channel name entered";
        return "bot joined channel \"" + channelName + "\"";
    }

    public List<String> getArgs() {
        List<String> args = new ArrayList<>();
        args.add("<channel>");

        return args;
    }

    public List<String> getArgInfo() {
        List<String> argInfo = new ArrayList<>();
        argInfo.add("name of voice channel");

        return argInfo;
    }

    public String getType() {
        return "Basic";
    }

    public String getDesc() {
        return "connects bot to a voice channel";
    }

    public boolean getAdmin() {
        return false;
    }

    public boolean getOwner() {
        return false;
    }

	// Determines if a voice channel name was specified. If so, join that channel, otherwise join the channel the user is in currently.
    public void execute(Guild guild, Member member, MessageChannel channel, String[] arg, List<Message.Attachment> attachments) {
        if (arg.length > 2)
        {
            StringBuilder sb = new StringBuilder();

            for (int i = 2; i < arg.length; i++)
            {
                sb.append(arg[i]);
                if (arg.length - 1 >i)
                    sb.append(" ");
            }
            channelName = sb.toString();

            try {
                Bot.joinVc(guild, channel, guild.getVoiceChannelsByName(channelName, true).get(0));
            } catch (IndexOutOfBoundsException e) {
                Bot.log(CommandObject.getLogType(), "voice channel \"" + channelName + "\" doesn't exist");
                channel.sendMessageEmbeds(new EmbedBuilder().addField("Unknown Voice Channel",
                        "\"" + channelName + "\" doesn't exist as a voice channel",
                        false)
                        .setColor(Color.red)
                        .build()).queue();
            }
        }
        else
        {
            try {
                channelName = Objects.requireNonNull(Objects.requireNonNull(member.getVoiceState()).getChannel()).getName();
                Bot.joinVc(guild, channel, member.getVoiceState().getChannel().asVoiceChannel());
            } catch (NullPointerException e) {
                channel.sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("No specified channel", "Be sure to either include the name of the channel, or join a voice channel and try the command again.", false)
                            .build())
                        .queue();
            }
        }
    }
}
