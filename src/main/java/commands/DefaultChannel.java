package commands;

import bot.Bot;
import commands.util.CommandObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Allows server admins to change the channel the bot sends messages
public class DefaultChannel extends CommandObject {

	private Guild guild;

    @Override
    public String getName() {
        return "channel";
    }

    @Override
    public String extraDetails() {
        try {
            return "Set default channel to " + Bot.defaultChannels.get(guild).getId();
        } catch (NullPointerException e) {
            return "Rejected request to set voice channel to default channel";
        }
    }

    @Override
    public List<String> getArgs() {
        List<String> args = new ArrayList<>();
        args.add("<channel>");

        return args;
    }

    @Override
    public List<String> getArgInfo() {
        List<String> argInfo = new ArrayList<>();
        argInfo.add("channel ID");

        return argInfo;
    }

    @Override
    public String getType() {
        return "Admin";
    }

    @Override
    public String getDesc() {
        return "change the channel I send messages to";
    }

    @Override
    public boolean getAdmin() {
        return true;
    }

    @Override
    public boolean getOwner() {
        return false;
    }

    @Override
    public void execute(Guild guild, Member member, MessageChannel channel, String[] arg, List<Message.Attachment> attachments) {

        if (!channel.getType().equals(ChannelType.TEXT))
        {
            channel.sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.red)
                        .addField("Invalid Channel Type", "Make sure the channel you want to make default is a text channel and not a voice channel." , false)
                        .build())
                    .queue();
            return;
        }

        TextChannel defaultChannel;
        this.guild = guild;

        if (arg.length < 3)
            defaultChannel = guild.getTextChannelById(channel.getId());
        else
            defaultChannel = guild.getTextChannelById(arg[2]);

        Bot.defaultChannels.put(guild, defaultChannel);
        Bot.aws.updateTableItem("SoundByteServerList", "ServerID", guild.getId(), "Default Channel", Bot.defaultChannels.get(guild).getId());

        assert defaultChannel != null;
        defaultChannel.sendMessageEmbeds(new EmbedBuilder()
                    .setColor(Color.green)
                    .addField("Success", "Set my default channel to " + Bot.defaultChannels.get(guild).getName(), false)
                    .build())
                .queue();
    }
}
