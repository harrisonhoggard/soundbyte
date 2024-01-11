package commands;

import commands.util.CommandObject;
import commands.util.JSHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Allow server admins to upload a server's rare join sound.
public class RareJS extends CommandObject {

    private String details;

    @Override
    public String getName() {
        return "rare";
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
        return "set a rare join sound (15 seconds or less)";
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
        if (arg.length >= 3)
        {
            textChannel.sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("Wrong format", "Remove any argument after \"rare\"", false)
                            .build())
                    .queue();
            details = "wrong format used";
            return;
        }

        if (JSHandler.uploadSound(textChannel, attachments, "rare.ogg", guild.getId() + "-joinsounds", 15000.0, 3000))
        {
            details = "added a new rare join sound";
            return;
        }

        details = "could not upload rare.ogg to S3 bucket \"" + guild.getId() + "-joinsounds\"";
    }
}
