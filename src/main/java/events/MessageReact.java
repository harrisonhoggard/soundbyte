package events;

import events.util.EventObject;
import bot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
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
}
