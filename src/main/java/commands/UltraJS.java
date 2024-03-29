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

// Allow server admins to assign their server's ultra-rare join sound.
public class UltraJS extends CommandObject {

    private String details;

    @Override
    public String getName() {
        return "ultra";
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
        return "set an ultra-rare join sound (40 seconds or less)";
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
                            .addField("Wrong format", "Remove any argument after \"ultra\"", false)
                            .build())
                    .queue();
            details = "wrong format used";
            return;
        }

        if (JSHandler.uploadSound(channel, attachments, "ultraRare.ogg", guild.getId() + "-joinsounds", 40000.0, 7000))
        {
            details = "added a new ultra rare join sound";
            return;
        }

        details = "could not upload ultraRare.ogg to S3 bucket \"" + guild.getId() + "-joinsounds\"";
    }
}
