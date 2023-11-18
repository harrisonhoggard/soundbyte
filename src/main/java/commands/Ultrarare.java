package commands;

import commands.util.CommandObject;
import bot.Bot;
import bot.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Retrieves information about a member, how many times they obtained, and last time they obtained the ultrarare role
public class Ultrarare extends CommandObject {

    private String memberName;

    @Override
    public String getName() {
        return "ultrainfo";
    }

    @Override
    public String extraDetails() {
        return "retrieved info for " + memberName;
    }

    @Override
    public List<String> getArgs() {
        List<String> args = new ArrayList<>();
        args.add("<person>");

        return args;
    }

    @Override
    public List<String> getArgInfo() {
        List<String> args = new ArrayList<>();
        args.add("the mention of the person");

        return args;
    }

    @Override
    public String getType() {
        return "Join Sound";
    }

    @Override
    public String getDesc() {
        return "view how long ago ultra rare status was given";
    }

    @Override
    public boolean getAdmin() {
        return false;
    }

    @Override
    public boolean getOwner() {
        return false;
    }

    // Retrieves information from the guild's ultrarare DynamoDB table
    @Override
    public void execute(Guild guild, Member member, TextChannel textChannel, String[] arg, List<Message.Attachment> attachments) {
        String tableName = guild.getId() + "-ultrarare";
        String currentHolder;
        Member memberToGet;

        if (arg.length < 3)
            memberToGet = member;
        else
        {
            try {
                memberToGet = guild.getMemberById(arg[2].replaceAll("[<@>]", ""));
            } catch (NumberFormatException e) {
                textChannel.sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.red)
                        .addField("Wrong format", "Be sure you use the mention for a person" +
                                "\n- Example: \"" + Config.get("COMMAND_PREFIX") + " ultra " + member.getAsMention() + "\"", false)
                        .build())
                    .queue();
                return;
            }
        }

        assert memberToGet != null;
        memberName = memberToGet.getEffectiveName();

        if (Bot.aws.getItem(tableName, "MemberID", memberToGet.getId()).isEmpty())
        {
            List<String> keys = new ArrayList<>();
            List<String> keyVals = new ArrayList<>();

            keys.add("MemberID");
            keyVals.add(memberToGet.getId());

            keys.add("Amount");
            keyVals.add("0");

            keys.add("Last Obtained");
            keyVals.add("Never");

            Bot.aws.addTableItem(tableName, keys, keyVals);
        }

        int amount = Integer.parseInt(Bot.aws.getItem(tableName, "MemberID", memberToGet.getId()).get("Amount").s());
        String lastObtained = Bot.aws.getItem(tableName, "MemberID", memberToGet.getId()).get("Last Obtained").s();

        tableName = "SoundByteServerList";
        if (Bot.aws.getItem(tableName, "ServerID", guild.getId()).get("Current Ultra") == null)
            Bot.aws.updateTableItem(tableName, "ServerID", guild.getId(), "Current Ultra", "<empty>");

        currentHolder = Bot.aws.getItem(tableName, "ServerID", guild.getId()).get("Current Ultra").s();
        if (currentHolder.compareTo("<empty>") == 0|| currentHolder.isEmpty())
            currentHolder = "No one";
        else
            currentHolder = Objects.requireNonNull(guild.getMemberById(currentHolder)).getAsMention();

        textChannel.sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.green)
                        .addField("Ultra Rare: " + memberToGet.getEffectiveName(),
                                "Times obtained: " + amount + "\n"
                                + "Last obtained: " + lastObtained + "\n\n"
                                + "Current holder: " + currentHolder,
                                false)
                        .build())
                .queue();
    }
}
