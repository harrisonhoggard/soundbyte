package commands;

import commands.util.CommandObject;
import bot.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Starts the shutdown process upon the owner executing the command from Discord
public class Shutdown extends CommandObject {

    private String user;
    
    public String getName() {
        return "shutdown";
    }

    public String extraDetails() {
        return user + "shut down bot;";
    }

    public List<String> getArgs() {
        return new ArrayList<>();
    }

    public List<String> getArgInfo() {
        return new ArrayList<>();
    }

    public String getType() {
        return "Owner";
    }

    public String getDesc() {
        return "shuts down the bot";
    }

    public boolean getAdmin() {
        return false;
    }

    public boolean getOwner() {
        return true;
    }

    public void execute(Guild guild, Member member, TextChannel textChannel, String[] arg, List<Message.Attachment> attachments) {
        this.user = member.getEffectiveName();

        super.devMessage(getName(), Arrays.toString(getArgs().toArray()).replaceAll("[\\[\\]]", ""), guild, user);

        Bot.shutdown(textChannel);
    }
}
