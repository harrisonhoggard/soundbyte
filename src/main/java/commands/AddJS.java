package commands;

import commands.util.CommandObject;
import commands.util.JSHandler;
import bot.Bot;
import bot.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AddJS extends CommandObject {

    private String details;
    private boolean admin = false;

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public String extraDetails() {
        return details;
    }

    @Override
    public List<String> getArgs() {
        List<String> args = new ArrayList<>();
        args.add("<person>");

        return args;
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
        return "set a person's join sound";
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
    public void execute(Guild guild, Member member, TextChannel textChannel, String[] arg, List<Message.Attachment> attachments) {
        User user;
        try {
            user = Objects.requireNonNull(guild.getMemberById(arg[2].replaceAll("[<@>]", ""))).getUser();
        } catch (ArrayIndexOutOfBoundsException e) {
            user = member.getUser();
        } catch (NumberFormatException e) {
            textChannel.sendMessageEmbeds(new EmbedBuilder()
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
            Bot.log(getLogType(), member.getEffectiveName() + " cannot execute add");
            textChannel.sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.red)
                        .addField("Permission denied", "You need to have the role: \"" + Config.get("ADMIN_ROLE") + "\" to use add on someone else.", false)
                        .build())
                    .queue();
            details = "admin privileges not possessed";
            return;
        }

        if (JSHandler.uploadSound(textChannel, attachments, user.getId() + ".ogg", guild.getId() + "-joinsounds", 10000.0, 200))
        {
            details = "added join sound for " + user.getEffectiveName();
            admin = false;

            return;
        }

        details = "could not upload sound to S3 bucket \"" + guild.getId() + "-joinsounds\"";
    }
}
