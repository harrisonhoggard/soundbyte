package commands;

import commands.util.CommandObject;
import commands.util.JSHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Assigns a server's default join sound different from the bot's default
public class DefaultJS extends CommandObject {

    private String details;

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public String extraDetails() {
        return details;
    }

    @Override
    public List<String> getArgs() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getArgInfo() {
        return new ArrayList<>();
    }

    @Override
    public String getType() {
        return "Join Sound";
    }

    @Override
    public String getDesc() {
        return "set a default join sound";
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
        if (arg.length >= 3)
        {
            channel.sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("Wrong format", "Remove any argument after **\"default\"**", false)
                            .build())
                    .queue();
            details = "wrong format used";
            return;
        }

        if (JSHandler.uploadSound(channel, attachments, "default.ogg", guild.getId() + "-joinsounds", 10000.0, 1800))
        {
            details = "added a new default join sound";
            return;
        }

        details = "could not upload default.ogg to S3 bucket \"" + guild.getId() + "-joinsounds\"";
    }
}
