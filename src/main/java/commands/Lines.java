package commands;

import commands.util.CommandObject;
import bot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Returns the number of lines in the code. I only used this for dev purposes (and to show off lol)
public class Lines extends CommandObject {

    private static int linesOfCode = 0;
    private static int numberOfFiles = 0;
    @Override
    public String getName() {
        return "lines";
    }

    @Override
    public String extraDetails() {
        return "";
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
        return "works only from IDE";
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
    public void execute(Guild guild, Member member, MessageChannel channel, String[] arg, List<Message.Attachment> attachments) {
        if (linesOfCode == 0)
        {
            File file = new File("src/main/java");
            File [] dir = file.listFiles();

            assert dir != null;
            recursiveFunction(dir, 0);
        }

        channel.sendMessageEmbeds(new EmbedBuilder()
                    .setColor(Color.cyan)
                    .addField("Lines of code", "I am made of " + linesOfCode + " lines of code within " + numberOfFiles + " files.", false)
                    .build())
                .queue();
    }

    // Adapted from my past project called "botTemplate"
    public void recursiveFunction(File [] file, int i) {
        // If there are no more files, stop the current recursive incident
        if (i == file.length)
            return;

        // If file is a folder, recursively look at the files inside
        if (file[i].isDirectory())
            recursiveFunction(Objects.requireNonNull(file[i].listFiles()), 0);

        // If file is not a folder, increment number of files and count the lines of code
        else
        {
            try {
                //noinspection resource
                linesOfCode += (int) Files.lines(Path.of(file[i].getPath())).count();
            } catch (IOException e) {
                Bot.log(getLogType(), "IOException");
            }

            numberOfFiles += 1;
        }

        recursiveFunction(file, ++i);
    }
}
