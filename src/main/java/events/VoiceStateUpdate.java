package events;

import audio.PlayerManager;
import events.util.EventObject;
import bot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

// Handles whenever a user joins a voice channel or disconnects from a channel.
public class VoiceStateUpdate extends EventObject {

    private Guild guild;

    @Override
    public String getName() {
        return "VoiceStateUpdate";
    }

    @Override
    public String getAction() {
        return "a voice channel";
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    // Determines what set of events to execute.
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        guild = event.getGuild();

        if (event.getMember().getUser().isBot())
            return;

        // Voice update handler.
        if (event.getOldValue() == null && event.getVoiceState().inAudioChannel())
            joinHandler(event);
        if (event.getNewValue() == null)
            leaveHandler(event);
    }

    // When someone joins a voice channel, their associated join sound is played.
    private void joinHandler(GuildVoiceUpdateEvent event) {
        VoiceChannel channelJoined = Objects.requireNonNull(event.getChannelJoined()).asVoiceChannel();

        if (!guild.getAudioManager().isConnected())
            guild.getAudioManager().openAudioConnection(channelJoined);

        Random rand = new Random();
        int randomInt = rand.nextInt(1000);

        URL url;
        try {
            if (randomInt < 1)
            {
                Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                                    .setColor(Color.CYAN)
                                    .addField("ULTRA-RARE STATUS ACHIEVED!", event.getMember().getAsMention() + " HAS SUCCESSFULLY ACHIEVED ULTRA-RARE STATUS!", false)
                                    .build())
                            .queue();

                url = new URL(Bot.aws.getObjectUrl(guild.getId() + "-joinsounds", "ultraRare.ogg"));

                PlayerManager.getInstance().getMusicManager(guild).audioPlayer.setVolume(75);

                String tableName = guild.getId() + "-ultrarare";
                if (Bot.aws.getItem(tableName, "MemberID", event.getMember().getId()).isEmpty())
                {
                    List<String> keys = new ArrayList<>();
                    List<String> keyVals = new ArrayList<>();

                    keys.add("MemberID");
                    keyVals.add(event.getMember().getId());

                    keys.add("Amount");
                    keyVals.add("1");

                    keys.add("Last Obtained");
                    keyVals.add(Bot.getLocalDate().format(DateTimeFormatter.ofPattern("M-d-y")));

                    Bot.aws.addTableItem(tableName, keys, keyVals);
                }
                else
                {
                    int updatedVal = Integer.parseInt(Bot.aws.getItem(tableName, "MemberID", event.getMember().getId()).get("Amount").s()) + 1;
                    Bot.aws.updateTableItem(tableName, "MemberID", event.getMember().getId(), "Amount", Integer.toString(updatedVal));
                    Bot.aws.updateTableItem(tableName, "MemberID", event.getMember().getId(), "Last Obtained", Bot.getLocalDate().format(DateTimeFormatter.ofPattern("M-d-y")));
                }

                tableName = "SoundByteServerList";
                Bot.aws.updateTableItem(tableName, "ServerID", guild.getId(), "Current Ultra", event.getMember().getId());
            }
            else if (randomInt < 50)
            {
                url = new URL(Bot.aws.getObjectUrl(guild.getId() + "-joinsounds", "rare.ogg"));

                PlayerManager.getInstance().getMusicManager(guild).audioPlayer.setVolume(60);
            }
            else {
                if (Bot.aws.verifyObject(guild.getId() + "-joinsounds", event.getMember().getId() + ".ogg"))
                    url = new URL(Bot.aws.getObjectUrl(guild.getId() + "-joinsounds", event.getMember().getId() + ".ogg"));
                else
                    url = new URL(Bot.aws.getObjectUrl(guild.getId() + "-joinsounds", "default.ogg"));

                PlayerManager.getInstance().getMusicManager(guild).audioPlayer.setVolume(60);
            }
        } catch (MalformedURLException e) {
            Bot.log(getLogType(), "Could not properly set URL");
            return;
        }

        PlayerManager.getInstance()
                .loadAndPlay(channelJoined, url.toString());

        devMessage(getName(),event.getMember().getEffectiveName() + " joined " + getAction() + " played " + url.getFile().replace("/", ""), getGuild());
    }

    // When everyone human user leaves a voice channel, the bot closes the connection as well.
    private void leaveHandler(GuildVoiceUpdateEvent event) {
        VoiceChannel channelLeft;

        try {
            channelLeft = Objects.requireNonNull(guild.getAudioManager().getConnectedChannel()).asVoiceChannel();
        } catch (NullPointerException e) {
            devMessage(getName(), event.getMember().getEffectiveName() + " left " + getAction(), getGuild());
            return;
        }

        final boolean[] isEmpty = {true};

        channelLeft.getMembers().forEach(member -> {
            if (!member.getUser().isBot())
                isEmpty[0] = false;
        });

        if (isEmpty[0])
            guild.getAudioManager().closeAudioConnection();

        devMessage(getName(), event.getMember().getEffectiveName() + " left " + getAction(), getGuild());
    }
}
