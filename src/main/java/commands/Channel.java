package commands;

import bot.Bot;
import commands.util.CommandObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Channel extends CommandObject {
    @Override
    public String getName() {
        return "channel";
    }

    @Override
    public String extraDetails() {
        return "";
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
        return "Basic";
    }

    @Override
    public String getDesc() {
        return "change the default channel I talk in";
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
    public void execute(Guild guild, Member member, TextChannel textChannel, String[] arg, List<Message.Attachment> attachments) {
        TextChannel defaultChannel;
        if (arg.length < 3)
            defaultChannel = textChannel;
        else
            defaultChannel = guild.getTextChannelById(arg[2]);

        Bot.defaultChannels.put(guild, defaultChannel);
        Bot.aws.updateTableItem("SoundByteServerList", "ServerID", guild.getId(), "Default Channel", Bot.defaultChannels.get(guild).getId());
        Bot.log(getLogType(), "Set default channel to " + Bot.defaultChannels.get(guild).getId());

        textChannel.sendMessageEmbeds(new EmbedBuilder()
                    .setColor(Color.cyan)
                    .addField("Success", "Set my default channel to " + Bot.defaultChannels.get(guild).getName(), false)
                    .build())
                .queue();
    }
}
