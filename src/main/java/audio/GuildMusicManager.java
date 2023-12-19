package audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

// Implemented from https://github.com/sedmelluq/lavaplayer/blob/707771af705b14ecc0c0ca4b5e5b6546e85f4b1c/demo-jda/src/main/java/com/sedmelluq/discord/lavaplayer/demo/jda/GuildMusicManager.java
public class GuildMusicManager {
    public final AudioPlayer audioPlayer;
    public final TrackScheduler scheduler;
    private final AudioPlayerSendHandler sendHandler;

    public GuildMusicManager(AudioPlayerManager playerManager) {
        this.audioPlayer = playerManager.createPlayer();
        this.scheduler = new TrackScheduler(this.audioPlayer);
        this.audioPlayer.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(this.audioPlayer);
    }

    public AudioPlayerSendHandler getSendHandler() {
        return sendHandler;
    }
}
