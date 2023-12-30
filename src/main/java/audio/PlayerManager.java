package audio;

import bot.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

// Handles the playing of audio files in each guild
// Influenced from examples at https://github.com/sedmelluq/lavaplayer/tree/707771af705b14ecc0c0ca4b5e5b6546e85f4b1c?tab=readme-ov-file#creating-an-audio-player-manager
public class PlayerManager {
    private static PlayerManager INSTANCE;
    private final AudioPlayerManager audioPlayerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    private String getLogType() {
        return "PLAYER MANAGER";
    }

    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager =  new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager);
            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    // If the bot is removed from the guild, the guild's music manager is removed from the map.
    // This is needed because if the bot is invited back into the guild in the same running instance as when it left,
    // usually it would not even play any audio. Removing the guild manager from the map fixes this.
    public void removeGuildManager(Guild guild) {
        musicManagers.remove(guild.getIdLong());
        Bot.log(getLogType(), "Removed " + guild.getId() + "'s music manager");
    }

    public void loadAndPlay(AudioChannel channel, String path) {
        final GuildMusicManager musicManager = this.getMusicManager(channel.getGuild());

        this.audioPlayerManager.loadItemOrdered(musicManager, path, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                musicManager.scheduler.queue(audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                for (AudioTrack track : audioPlaylist.getTracks())
                {
                    musicManager.scheduler.queue(track);
                }
            }

            @Override
            public void noMatches() {
                Bot.defaultChannels.get(channel.getGuild()).sendMessageEmbeds(
                        new EmbedBuilder()
                                .setColor(Color.red)
                                .addField("Error", "Could not play due to \"No Match\" error", false)
                                .build()
                ).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                Bot.defaultChannels.get(channel.getGuild()).sendMessageEmbeds(
                        new EmbedBuilder()
                                .setColor(Color.red)
                                .addField("Error", "Loading failed because the join sound doesn't exist", false)
                                .build()
                ).queue();
            }
        });
    }

    public static PlayerManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerManager();
        }

        return INSTANCE;
    }
}
