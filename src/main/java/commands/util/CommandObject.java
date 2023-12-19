package commands.util;

import java.awt.*;
import java.util.*;
import java.util.List;

import commands.*;
import bot.Bot;
import bot.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

// The blueprint for commands. Their initialization is contained here.
public abstract class CommandObject {

    public static Map<String, List<CommandObject>> types;
    public static Map<String, CommandObject> commands;

	// Returns certain details for each command.
    public abstract String getName();
    public abstract String extraDetails();
    public abstract List<String> getArgs();
    public abstract List<String> getArgInfo();
    public abstract String getType();
    public abstract String getDesc();
    public abstract boolean getAdmin();
    public abstract boolean getOwner();

	// Blueprint for each command's execution.
    public abstract void execute(Guild guild, Member member, TextChannel textChannel, String [] arg, List<Message.Attachment> attachments);

	// The name that is printed in the logger.
    public static String getLogType() {
        return "COMMAND";
    }

	// Initializes the commands in a hash map, allowing for easy and efficient retrieval.
    public static void init() {
        types = new HashMap<>();
        commands = new HashMap<>();

        // Add any new commands to the hash map here
        commands.put("help", new Help());
        commands.put("shutdown", new Shutdown());
        commands.put("join", new Join());
        commands.put("leave", new Leave());
        commands.put("lines", new Lines());
        commands.put("add", new AddJS());
        commands.put("remove", new RemoveJS());
        commands.put("default", new DefaultJS());
        commands.put("rare", new RareJS());
        commands.put("ultra", new UltraJS());
        commands.put("ultrainfo", new Ultrainfo());
        commands.put("test", new Test());
        commands.put("channel", new Channel());
        commands.put("log", new Log());

        initTypes();
        Bot.log(getLogType(), "Commands successfully initialized");
    }

	// Each command has a category (type) associated with it. This "types" map is initialized here.
	// The map initialized here is used when calling the help command, which prints commands grouped by their type.
    private static void initTypes() {
        commands.forEach((commandName, command) -> {
            if (!types.containsKey(command.getType()))
                types.put(command.getType(), new ArrayList<>());
            
            types.get(command.getType()).add(command);

        });
    }
    
    // The developer message that gets logged after each command executes.
    public void devMessage(String name, String details, Guild guild, String member) {
        Bot.log(getLogType(), guild.getName() + ": " + member + " executed " + name + "; " + details);
    }

    // Determines whether a user has access to admin privileged commands.
    public boolean adminPriv(Member member) {
        try {
            Role adminRole = member.getGuild().getRolesByName(Config.get("ADMIN_ROLE"), true).get(0);

            if (getAdmin())
                return member.getRoles().contains(adminRole);

            return true;
        } catch (IndexOutOfBoundsException e) {
            Bot.defaultChannels.get(member.getGuild()).sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.red)
                        .addField(Config.get("ADMIN_ROLE") + " role does not exist.", "There is no role called \"" +
                                Config.get("ADMIN_ROLE") + "\", which means I can't work properly. Be sure to create one before attempting to use any commands.", false)
                        .build())
                    .queue();
            return false;
        }
    }

    // Determines whether a user has access to owner privileged commands.
    public boolean ownerPriv(Member member) {
        if (getOwner())
            return (member.getId().compareTo(Config.get("OWNER_ID")) == 0);

        return true;
    }
}
