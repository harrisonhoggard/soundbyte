package events;

import events.util.EventObject;
import bot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

// Respond to prompts stored in DynamoDB table.
public class MessageReact extends EventObject {

    private Guild guild;
    private Member member;

    @Override
    public String getName() {
        return "MessageReact";
    }

    @Override
    public String getAction() {
        return ("message from " + member.getEffectiveName());
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (event.getAuthor().isBot())
            return;

        if (!event.getMessage().isFromGuild())
        {
            Bot.log(getLogType(), "Direct message from " + Objects.requireNonNull(event.getAuthor().getEffectiveName()) + ": \"" + event.getMessage().getContentRaw() + "\"");
            event.getChannel().asPrivateChannel().sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.cyan)
                            .setDescription("Hello " + event.getAuthor().getEffectiveName() + ", " +
                                    "\n\nIf you want to invite me to your server, click on the link in my profile description.")
                            .build())
                    .queue();
            return;
        }

        guild = event.getGuild();

        if (!event.getChannelType().equals(ChannelType.TEXT))
        {
            Bot.log(getLogType(), guild.getId() +  ": Message " + event.getMessageId() + " came from channel type \"" + event.getChannelType() + "\"");
            return;
        }

        // Some bot profiles are null(?) accounts, and it results in a NullPointerException when they send messages. This fixes it.
        if (event.getMember() == null)
            return;

        member = event.getMember();

        if (Objects.requireNonNull(event.getMember()).getUser().isBot())
            return;

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

        // There is a chance attribute for each response. If you want the bot to respond to a message everytime, set "Chance" to 1.
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
}
