package commands;

import commands.util.CommandObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Allows me to test any features I want to add
public class Test extends CommandObject {
    @Override
    public String getName() {
        return "test";
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
        return "Dev";
    }

    @Override
    public String getDesc() {
        return "test out new features";
    }

    @Override
    public boolean getAdmin() {
        return false;
    }

    @Override
    public boolean getOwner() {
        return true;
    }

    // CURRENTLY: Testing time based reactions
    @Override
    public void execute(Guild guild, Member member, MessageChannel channel, String[] arg, List<Message.Attachment> attachments) {
        channel.sendMessageEmbeds(new EmbedBuilder()
                    .setColor(Color.cyan)
                    .addField("Test 1", "Testing a timer", false)
                    .build())
                .queue(message -> {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                        message.addReaction(Emoji.fromUnicode("✅")).queue();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
