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
        return "add an ultra-rare join sound";
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
        if (!attachments.isEmpty())
            textChannel.deleteMessageById(textChannel.getLatestMessageId()).queue();

        if (arg.length >= 3)
        {
            textChannel.sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("Wrong format", "Remove any argument after \"ultra\"", false)
                            .build())
                    .queue();
            details = "wrong format used";
            return;
        }

        if (!JSHandler.uploadSound(textChannel, attachments, "ultraRare.ogg", guild.getId() + "-joinsounds", 40000.0, 800))
        {
            details = "added a new ultra rare join sound";
            return;
        }

        details = "could not upload ultraRare.ogg to S3 bucket \"" + guild.getId() + "-joinsounds\"";
    }
}
