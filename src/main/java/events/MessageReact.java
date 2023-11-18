package events;

import events.util.EventObject;
import bot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// In charge of two things:
//      1. Delete and respond to toxic messages.
//      2. Respond to prompts stored in DynamoDB table.
public class MessageReact extends EventObject {

    private final static Map<Long, Long> guildToxicBlock = new HashMap<>();
    private Guild guild;

    @Override
    public String getName() {
        return "MessageReact";
    }

    @Override
    public String getAction() {
        return "message in guild \"" + guild.getName() + "\"";
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        guild = event.getGuild();

        if (Objects.requireNonNull(event.getMember()).getUser().isBot())
            return;

        /*if (isToxic(event.getMessage().getContentRaw()))
        {
            if (Bot.aws.getItem("SoundByteServerList", "ServerID", event.getGuild().getId()).get("Block Toxic Comments") == null)
            {
                event.getChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.cyan)
                            .addField("Server Preference", "Do you want to block toxic comments in the future?", false)
                            .build())
                        .queue(message -> {
                            message.addReaction(Emoji.fromUnicode("✅")).queue();
                            message.addReaction(Emoji.fromUnicode("❌")).queue();

                            guildToxicBlock.put(event.getGuild().getIdLong(), message.getIdLong());
                        });
                return;
            }

            String blockToxicComments = Bot.aws.getItem("SoundByteServerList", "ServerID", event.getGuild().getId()).get("Block Toxic Comments").s();

            if (blockToxicComments.compareTo("true") == 0)
            {
                event.getMessage().delete().queue();
                event.getChannel().asTextChannel().sendMessage(Bot.aws.getObjectUrl(Config.get("BOT_NAME") + "-photos", "penguins.jpg")).queue();

                devMessage(getName(), "Blocked " + getAction(), getGuild());
            }

            return;
        }*/

        Map<String, AttributeValue> item = null;
        ScanResponse response = Bot.aws.scanItems("SoundByteResponses");

        for (Map<String, AttributeValue> entry : response.items())
        {
            if (event.getMessage().getContentRaw().toLowerCase().contains(entry.get("Prompt").s().toLowerCase()))
            {
                item = entry;
                break;
            }
        }

        if (item == null)
            return;

        Random rand = new Random();
        int randomInt = rand.nextInt(Integer.parseInt(item.get("Chance").n())) + 1;

        if (randomInt != 1)
            return;

        if (item.get("Reaction") != null)
        {
            event.getChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .addField("Dear " + event.getMember().getEffectiveName() + ":", item.get("Reaction").s(), false)
                        .build())
                    .queue();

            devMessage(getName(), "Reacted to \"" + item.get("Prompt").s() + "\" " + getAction(), getGuild());
        }
        if (item.get("Send photo") != null && item.get("Send photo").s().compareTo("true") == 0)
        {
            event.getChannel().asTextChannel().sendMessage(item.get("Photo path").s()).queue();
            devMessage(getName(), "Sent a photo reacting to \"" + item.get("Prompt").s() + "\" " + getAction(), getGuild());
        }
    }

    // Handles the reaction to the message the bot sends a guild, asking whether they want to block toxic comments or not.
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (Objects.requireNonNull(event.getMember()).getUser().isBot() || !guildToxicBlock.containsKey(event.getGuild().getIdLong()))
            return;

        if (guildToxicBlock.get(event.getGuild().getIdLong()) == event.getMessageIdLong())
        {
            if (event.getEmoji().getName().equals("✅"))
            {
                Bot.aws.updateTableItem("SoundByteServerList", "ServerID", event.getGuild().getId(), "Block Toxic Comments", "true");
                event.getChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.cyan)
                            .addField("Server Preference", "I will block toxic comments for this server.", false)
                            .build())
                        .queue();
                Bot.log(getLogType(), event.getGuild().getName() + " decided to start blocking toxic comments");
            }
            else if (event.getEmoji().getName().equals("❌"))
            {
                Bot.aws.updateTableItem("SoundByteServerList", "ServerID", event.getGuild().getId(), "Block Toxic Comments", "false");
                event.getChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.cyan)
                            .addField("Server Preference", "I will not block toxic comments for this server.", false)
                            .build())
                        .queue();
                Bot.log(getLogType(), event.getGuild().getName() + " decided to not block toxic comments");
            }

            guildToxicBlock.remove(event.getGuild().getIdLong());
        }
    }

    // Determines if a message is toxic.
    @SuppressWarnings("unused")
    private boolean isToxic(String message) {

        String regExPattern = "(?i)(n\\S{4,}(r|(u(\\s*)h))|n\\S{3,}a)|n(\\s*)i(\\s*)g(\\s]*g(\\s*)(a|(u(\\s*)h)|(e(\\s*)r)))";
        Pattern r = Pattern.compile(regExPattern);
        Matcher matcher = r.matcher(message);

        return matcher.find();
    }
}
