package commands;

import commands.util.CommandObject;
import bot.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Prints out either a list of commands grouped by their type, or a single specified command.
public class Help extends CommandObject {

    private boolean isSingle = false;
    private String singleCommand;
    
    public String getName() {
        return "help";
    }

    public String extraDetails() {
        if (!isSingle)
            return "";
        isSingle = false;
        return "requested command \"" + singleCommand + "\";";
    }

    public List<String> getArgs() {
        List<String> args = new ArrayList<>();
        args.add("<command>");

        return args;
    }

    public List<String> getArgInfo() {
        List<String> argInfo = new ArrayList<>();
        argInfo.add("name of command");

        return argInfo;
    }

    public String getType() {
        return "Basic";
    }

    public String getDesc() {
        return "display command information";
    }

    public boolean getAdmin() {
        return false;
    }

    public boolean getOwner() {
        return false;
    }

	// Before printing, determines whether one single command needs to printed, or all of them. 
    public void execute(Guild guild, Member member, MessageChannel channel, String[] arg, List<Message.Attachment> attachments) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Commands: " + Config.get("COMMAND_PREFIX") + " <command>");
        eb.setColor(Color.cyan);

        if (arg.length >= 3)
            singleEmbedBuild(arg[2], member, eb);
        else
            allEmbedBuild(member, eb);

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    // Prints a specified command if the user has access to it.
    private void singleEmbedBuild(String cmd, Member member, EmbedBuilder eb) {
        isSingle = true;
        singleCommand = cmd;
        StringBuilder sb = new StringBuilder();

        if (!CommandObject.commands.containsKey(cmd))
        {
            eb.addField(member.getEffectiveName(),
                    "Command \"" + cmd + "\" has not been found. Be sure to use \"" + Config.get("COMMAND_PREFIX") + " help\" for more info",
                    false);
            return;
        }

        CommandObject commandArgs = CommandObject.commands.get(cmd);
        for (String arg : commandArgs.getArgs())
        {
            sb.append("\n*    ").append(arg).append(": ").append(commandArgs.getArgInfo().get(commandArgs.getArgs().indexOf(arg)));
        }

        // Security through redundancy
        CommandObject command = CommandObject.commands.get(cmd);
        if (command.ownerPriv(member) && command.adminPriv(member))
        {
            eb.addField(command.getType(),
                    "**" + command.getName() + "** "
                            + Arrays.toString(command.getArgs().toArray()).replace(",", " ").replaceAll("[\\[\\]]", "") + ": "
                            + command.getDesc() + " "
                            + sb,
                    false);
            sb.delete(0, sb.length());
        }
        else
            eb.setColor(Color.red).addField("Permission denied", "You do not have the required \"" + Config.get("ADMIN_ROLE") +
                    "\" role to see **" + cmd + "**. Be sure to use \"" + Config.get("COMMAND_PREFIX") +
                    " help\" for commands you can use", false);

    }

    // Prints all the commands the user has permission to use.
    private void allEmbedBuild(Member member, EmbedBuilder eb) {
        StringBuilder sb = new StringBuilder();

        boolean [] canPrint = {false} ;

        CommandObject.types.forEach((type, commands) ->
        {
            for (CommandObject command : commands)
            {
                if (command.ownerPriv(member) && command.adminPriv(member))
                {

                    canPrint [0] = true;
                    sb.append("\n**").append(command.getName()).append("** ").append(Arrays.toString(command.getArgs().toArray()).replaceAll("[\\[\\]]", "")).append(": ").append(command.getDesc());

                    for (String arg : command.getArgs())
                    {
                        sb.append("\n*    ").append(arg).append(": ").append(command.getArgInfo().get(command.getArgs().indexOf(arg)));
                    }
                }
            }

            if (canPrint[0])
            {
                eb.addField(type, sb.toString(), false);
                sb.delete(0, sb.length());
            }
            canPrint[0] = false;
        });
    }
}
