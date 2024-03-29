package commands;

import commands.util.CommandObject;
import bot.Bot;
import bot.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RemoveJS extends CommandObject {

    private boolean admin = false;
    private String details;

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String extraDetails() {
        return details;
    }

    @Override
    public List<String> getArgs() {
        List<String> arg = new ArrayList<>();
        arg.add("<person>");
        return arg;
    }

    @Override
    public List<String> getArgInfo() {
        List<String> argInfo = new ArrayList<>();
        argInfo.add("the mention of the person");

        return argInfo;
    }

    @Override
    public String getType() {
        return "Join Sound";
    }

    @Override
    public String getDesc() {
        return "remove a person's join sound";
    }

    @Override
    public boolean getAdmin() {
        return admin;
    }

    @Override
    public boolean getOwner() {
        return false;
    }

    @Override
    public void execute(Guild guild, Member member, MessageChannel channel, String[] arg, List<Message.Attachment> attachments) {
        User user;
        try {
            user = Objects.requireNonNull(guild.getMemberById(arg[2].replaceAll("[<@>]", ""))).getUser();
        } catch (ArrayIndexOutOfBoundsException e) {
            user = member.getUser();
        } catch (NumberFormatException e) {
            channel.sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("Wrong format", "Be sure you use the mention for a person" +
                                    "\n- Example: \"" + Config.get("COMMAND_PREFIX") + " " + getName() + " " + member.getAsMention() + "\"", false)
                            .build())
                    .queue();
            details = "person's mention not used";
            return;
        }

        if (user != member.getUser())
            admin = true;

        if (!adminPriv(member))
        {
            Bot.log(getLogType(), member.getEffectiveName() + " cannot execute remove");
            channel.sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("Permission denied", "You need to have the role: \"" + Config.get("ADMIN_ROLE") + "\" to use remove on someone else.", false)
                            .build())
                    .queue();
            details = "admin privileges not possessed";
            admin = false;

            return;
        }

        if (!Bot.aws.verifyObject(guild.getId() + "-joinsounds", user.getId() + ".ogg"))
        {
            channel.sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("Failure", "Cannot delete sound file, because it does not exist", false)
                            .build())
                    .queue();
            details = "sound file doesn't exist; cannot be removed";
            admin = false;

            return;
        }

        Bot.aws.deleteObject(guild.getId() + "-joinsounds", user.getId() + ".ogg");
        channel.sendMessageEmbeds(new EmbedBuilder()
                .setColor(Color.green)
                .addField("Success", "Deleted join sound file for " + user.getAsMention(), false).build()).queue();

        details = "removed join sound for " + user.getEffectiveName();
        admin = false;
    }
}
