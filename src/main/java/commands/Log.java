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

// Saves and uploads the log for the day so far.
public class Log extends CommandObject {
    @Override
    public String getName() {
        return "log";
    }

    @Override
    public String extraDetails() {
        return "saved log for " + Bot.getDate();
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
        return "Owner";
    }

    @Override
    public String getDesc() {
        return "saves and uploads log";
    }

    @Override
    public boolean getAdmin() {
        return false;
    }

    @Override
    public boolean getOwner() {
        return true;
    }

    @Override
    public void execute(Guild guild, Member member, TextChannel textChannel, String[] arg, List<Message.Attachment> attachments) {
        Bot.log(getLogType(), "Saving log by command");
        Bot.writeLog();
        textChannel.sendMessageEmbeds(new EmbedBuilder()
                    .setColor(Color.cyan)
                    .addField("Log Saved", Bot.getDate() + ".log was saved.", false)
                    .build())
                .queue();
    }
}
