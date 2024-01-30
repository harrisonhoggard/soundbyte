package commands.util;

import bot.Bot;
import bot.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;
import java.util.List;

// Handles the parsing of commands and executing them. 
public class CommandHandler {

	// The name that is printed in the logger.
    private String getLogType() {
        return "COMMAND HANDLER";
    }

    // Parses commands, and determines whether user has privileges to use them.
    public CommandHandler(Guild guild, MessageChannel channel, Member member, String [] arg, List<Message.Attachment> attachments) {
        String cmd = arg[1];

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Commands: " + Config.get("COMMAND_PREFIX") + "<command>");
        eb.setColor(Color.cyan);

        if (CommandObject.commands.containsKey(cmd))
        {
            CommandObject command = CommandObject.commands.get(cmd);

            if (command.adminPriv(member) && command.ownerPriv(member))
            {
                command.execute(guild, member, channel, arg, attachments);
                command.devMessage(command.getName(), command.extraDetails(), guild, member.getEffectiveName());
            }
            else if (guild.getRolesByName(Config.get("ADMIN_ROLE"), true).isEmpty())
            {
                eb.addField(guild.getName() + ": " + Config.get("ADMIN_ROLE") + " role does not exist", "In order to execute this command, " +
                        "a role called " + Config.get("ADMIN_ROLE") + " must be created. Make sure only trusted people have this role", false);
                channel.sendMessageEmbeds(eb.setColor(Color.red).build()).queue();
            }
            else
            {
                eb.addField(guild.getName() + ": Permission denied", "You do not have the required \"" + Config.get("ADMIN_ROLE") +
                        "\" role to use \"" + cmd + "\" Be sure to use \"" + Config.get("COMMAND_PREFIX") +
                        " help\" for commands you can use", false);
                channel.sendMessageEmbeds(eb.setColor(Color.red).build()).queue();
            }
        }
        else
        {
            Bot.log(getLogType(), guild.getId() + ": could not find command \"" + cmd + "\"");
            eb.addField(member.getEffectiveName(), "I don't know that command. Type in \"" + Config.get("COMMAND_PREFIX") +
                    " help\" for help on commands.", true);
            channel.sendMessageEmbeds(eb.setColor(Color.red).build()).queue();
        }
    }
}
