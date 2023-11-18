package bot;

import AWSHandling.AWSHandler;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import commands.util.CommandObject;
import commands.util.DiscordHandler;
import events.util.EventObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

// Main method contained here. Responsible for initialization of everything, as well as storing all the helper methods that are needed throughout the entire program.
public class Bot extends ListenerAdapter{

	public static JDA jda;

    private static Logger logger;
    public static AWSHandler aws;

    private static final Map<Long, Long> devChannel = new HashMap<>();

    private static final EventWaiter eventWaiter = new EventWaiter();

    // Starts the customized logger.
    private void logStart() {
        logger = new Logger();
    }

    // The name that is printed in the logger.
    private static String getLogType() {
        return "BOT";
    }

    // Initializes anything required for AWS.
    private void awsInit() {
        aws = new AWSHandler();
    }

    // Initializes S3 Buckets, since I need bot name to be initialized before this.
    private void awsInitBucket() {
        aws.initS3Bucket(Config.get("BOT_NAME") + "-logs");

        aws.initS3Bucket(Config.get("BOT_NAME") + "-photos");
        helpUpload(Config.get("BOT_NAME") + "-photos", "penguins.jpg", "/photoFiles/penguins.jpg");
    }

    // Initializes anything required for bot configuration.
    private void loadConfig() {
        if (!Config.isInitialized())
        {
            Config.init();
        }

        Bot.log(getLogType(), "Bot initialized?: " + Config.isInitialized());
        if (!Config.isInitialized())
            shutdown();
    }

    // Initializes the JDA variable and everything else required for the bot to run.
    private boolean startBot() throws InterruptedException {
    	if (Config.get("TOKEN").isEmpty())
            return false;

        try {
            jda = JDABuilder
                    .createDefault(Config.get("TOKEN"), EnumSet.allOf(GatewayIntent.class))
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(EnumSet.allOf(CacheFlag.class))
                    .enableIntents(EnumSet.allOf(GatewayIntent.class))
                    .addEventListeners(eventWaiter)
                    .build();
        } catch (RuntimeException e) {
            Bot.log(getLogType(), "Could not initialize JDA object.");
            return false;
        }

        jda.awaitReady();

        Config.destroyToken();

        setActivity(Config.get("ACTIVITY"));

        for (Guild guild : jda.getGuilds())
        {
            guildInit(guild);
        }

        CommandObject.init();

        EventObject.init();

        new Calendar();

        jda.addEventListener(this);

        return true;
    }

    // Static call used to log events when needed.
    public static void log(String type, String msg) {
        logger.log(type, msg);
    }

    // Static call used to write the current day's log to a file, and start a new day's log.
    public static void writeLog() {
        logger.writeToFile();
    }

    // Handles accessibility of files inside .jar file and uploads them to the appropriate bucket.
    private static void helpUpload(String bucketName, String fileName, String filePath) {
        if (!aws.verifyObject(bucketName, fileName))
        {
            InputStream inputStream = Bot.class.getResourceAsStream(filePath);
            File tempFile = new File(fileName);
            try {
                //noinspection ResultOfMethodCallIgnored
                tempFile.createNewFile();
                assert inputStream != null;
                FileUtils.copyInputStreamToFile(inputStream, tempFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            aws.uploadObject(bucketName, fileName, tempFile.getPath());
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Initializes everything required for each Discord server in which the bot is present.
    public static void guildInit(Guild guild) {

        List<String> keys = new ArrayList<>();
        List<String> keyVals = new ArrayList<>();

        keys.add("ServerID");
        keyVals.add(guild.getId());

        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
        {
            Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.red)
                        .addField("Insufficient permissions", "In order to work as intended, I'll need the Manage Roles permission. I'll continue initializing after it's given to me", false)
                        .build())
                    .queue(message -> eventWaiter.waitForEvent(
                            GuildMemberRoleAddEvent.class,
                            e -> guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES),
                            e -> {
                                Objects.requireNonNull(e.getGuild().getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                                                .setColor(Color.green)
                                                .addField("Thank you", "Continuing initialization", false)
                                                .build())
                                        .queue();
                                guildInit(guild);
                            }
                    ));
            return;
        }

        // Creates the required admin role if it does not yet exist.
        if (guild.getRolesByName(Config.get("ADMIN_ROLE"), true).isEmpty())
        {
            try {
                guild.createRole()
                        .setName(Config.get("ADMIN_ROLE"))
                        .setColor(Color.red)
                        .setMentionable(true)
                        .complete();

                log(getLogType(), "Created " + Config.get("ADMIN_ROLE") + " role in guild " + guild.getName());

                guild.addRoleToMember(Objects.requireNonNull(guild.getOwner()), guild.getRolesByName(Config.get("ADMIN_ROLE"), true).get(0))
                        .complete();
                log(getLogType(), "Gave the guild owner the " + Config.get("ADMIN_ROLE") + " role");

                Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                                .setColor(Color.cyan)
                                .addField(Config.get("ADMIN_ROLE") + " role created", "Created " + guild.getRolesByName(Config.get("ADMIN_ROLE"), true).get(0).getAsMention() + " role.", false)
                                .build())
                        .queue();
            } catch (InsufficientPermissionException e) {
                Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("Insufficient permissions", "I tried to create a new " + Config.get("ADMIN_ROLE") + " role, but I need permission to manage roles.", false)
                            .build())
                        .queue();
                Bot.log(getLogType(), "Tried to create the " + Config.get("ADMIN_ROLE") + " role, but I don't have the Manage Roles permission.");

                eventWaiter.waitForEvent(PermissionOverrideCreateEvent.class,
                        event -> guild.getSelfMember().equals(event.getMember()),
                        event -> Objects.requireNonNull(event.getGuild().getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                                    .setColor(Color.green)
                                    .addField("Thank you", "Trying to create the " + Config.get("ADMIN_ROLE") + " again", false)
                                    .build())
                                .queue());
            }
        }
        if (guild.getRolesByName("Bot", true).isEmpty())
        {
            try {
                guild.createRole()
                        .setName("Bot")
                        .setColor(Color.blue)
                        .setMentionable(true)
                        .complete();

                log(getLogType(), "Created bot role in guild " + guild.getName());

                Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                                .setColor(Color.cyan)
                                .addField("Bot role created", "Be sure to move the " + guild.getRolesByName("Bot", true).get(0).getAsMention() + " role to the top, in order for me to work as intended.", false)
                                .build())
                        .queue();

                guild.addRoleToMember(jda.getSelfUser(), guild.getRolesByName("Bot", true).get(0)).queue();
            } catch (InsufficientPermissionException e) {
                Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("Insufficient permissions", "I tried to create a new bot role for myself, but I need permission to manage role.", false)
                            .build())
                        .queue();
                Bot.log(getLogType(), "Tried to create the bot role, but I don't have the Manage Roles permission.");

                eventWaiter.waitForEvent(PermissionOverrideCreateEvent.class,
                        event -> guild.getSelfMember().equals(event.getMember()),
                        event -> Objects.requireNonNull(event.getGuild().getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                                        .setColor(Color.green)
                                        .addField("Thank you", "Trying to create the " + Config.get("ADMIN_ROLE") + " again", false)
                                        .build())
                                .queue());
            }
        }

        if (aws.getItem("SoundByteServerList", "ServerID", guild.getId()).isEmpty())
        {
            aws.addTableItem("SoundByteServerList", keys, keyVals);

            Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .addField(Config.get("BOT_NAME"), "Hello, thank you for inviting me to your server. I am capable of playing short sounds over voice chat whenever" +
                                " someone connects to a channel. There are default sounds that apply to everyone, but you can customize them if you want.", false)
                        .build())
                    .queue();
            guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .addField("Commands", "In order to upload a sound, just type in \"" + Config.get("COMMAND_PREFIX") + " add\", and then attach a sound file with the extension \".ogg\". If you're not sure how to convert one, just " +
                                "click on this link to learn how to use Audacity to accomplish this https://www.cedarville.edu/insights/computer-help/post/convert-audio-files.", false)
                        .build())
                    .queue();
            guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .addField("Commands (cont.)", "You can also remove with \"" + Config.get("COMMAND_PREFIX") + " remove\", and you can add other people's sounds if you have the " + guild.getRolesByName(Config.get("ADMIN_ROLE"), false).get(0).getAsMention()+
                                "role by entering \"" + Config.get("COMMAND_PREFIX") + " add @someone\", and you can also remove sounds the same way.", false)
                        .build())
                    .queue();
            guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .addField("Rare and Ultra-rare", "There are two other tiers of sounds: a rare, and an ultra-rare. These are supposed to be special sounds that play randomly, and these can also be customized for " +
                                "your server. Just use \"" + Config.get("COMMAND_PREFIX") + " rare\" and \"" + Config.get("COMMAND_PREFIX") +  " ultra\" with an attached sound to change them.", false)
                        .build())
                    .queue();
            guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .addField("Contact", "If you have any questions, or need to report any problems, feel free to contact me at " +
                                "\"crispycrusaderdev@gmail.com\"", false)
                        .build())
                    .queue();
            guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .addField("Initializing", "Give me a few moments to setup everything.", false)
                        .build())
                    .queue();

            log(getLogType(), "Sent tutorial to server \"" + guild.getName() + "\"");
        }
        
		// Creates the required dev channel that the bot can send messages.
        /*if (guild.getTextChannelsByName("dev-channel", true).isEmpty() && (aws.getItem("SoundByteServerList", "ServerID", guild.getId()).get("Dev Channel Preference") == null || aws.getItem("SoundByteServerList", "ServerID", guild.getId()).get("Dev Channel Preference").s().compareTo("false") != 0))
        {
            guild.createTextChannel("dev-channel")
                    .complete();

            TextChannel textChannel = guild.getTextChannelsByName("dev-channel", true).get(0);

            textChannel.getManager()
                    .putRolePermissionOverride(guild.getPublicRole().getIdLong(), null, Collections.singleton(Permission.VIEW_CHANNEL))
                    .complete();

            textChannel.getManager()
                    .setTopic("The channel that " + Config.get("BOT_NAME") + " created. Dev messages will be sent here.")
                    .complete();

            textChannel.sendMessageEmbeds(new EmbedBuilder().setColor(Color.cyan)
                        .addField("ATTENTION", "I created this channel to send dev messages. I highly recommend you do not delete this channel.\n If you want to keep it, react with ✅.\nTo delete it, react with ❌.", false)
                        .build())
                    .queue(message -> {
                        message.addReaction(Emoji.fromUnicode("✅")).queue();
                        message.addReaction(Emoji.fromUnicode("❌")).queue();

                        devChannel.put(guild.getIdLong(), message.getIdLong());
                    });

            log(getLogType(), "Created dev text channel in guild " + guild.getName());
        }
        else {
            aws.updateTableItem("SoundByteServerList", "ServerID", guild.getId(), "Dev Channel Preference", "true");
        }*/

        // Creates each guild's join sound bucket if it does not yet exist.
        String bucketName = guild.getId() + "-joinsounds";
        aws.initS3Bucket(bucketName);
        helpUpload(bucketName, "default.ogg", "/audioFiles/default.ogg");
        helpUpload(bucketName, "rare.ogg", "/audioFiles/rare.ogg");
        helpUpload(bucketName, "ultraRare.ogg", "/audioFiles/ultraRare.ogg");

		// Creates each guild's table for storing ultrarare information
        String tableName = guild.getId() + "-ultrarare";
        AttributeDefinition tableAttributes = AttributeDefinition.builder()
                .attributeName("MemberID")
                .attributeType("S")
                .build();
        boolean result = aws.verifyDynamoTable(tableName);

        if (!result) {
            log(getLogType(), tableName + " table does not exist, creating now...");
            aws.createDynamoTable(tableName, tableAttributes);
            log(getLogType(), tableName + " table was created");

            Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.green)
                        .addField("Initialization completed", "You can now use me. Type in \"" + Config.get("COMMAND_PREFIX") + " help\" to get started.", false)
                        .build())
                    .queue();
        }

        keys.clear();
        keyVals.clear();
    }

    // Static call for joining a voice channel.
    public static void joinVc(Guild guild, TextChannel textChannel, VoiceChannel voiceChannel) {
        AudioManager audioManager = guild.getAudioManager();

        if (audioManager.isConnected() && Objects.requireNonNull(audioManager.getConnectedChannel()).asVoiceChannel().equals(voiceChannel))
        {
            textChannel.sendMessageEmbeds(new EmbedBuilder().addField("", "Already connected to voice channel \"" + voiceChannel.getName() + "\"", false).setColor(Color.yellow).build()).queue();
            return;
        }

        try {
            audioManager.openAudioConnection(voiceChannel);
        } catch(Exception e) {
            textChannel.sendMessageEmbeds(new EmbedBuilder().addField("", "Could not join voice channel \"" + voiceChannel.getName(), false).setColor(Color.red).build()).queue();
        }
    }

    // Static call for leaving a voice channel.
    public static void leaveVc(Guild guild, TextChannel textChannel) {
        AudioManager audioManager = guild.getAudioManager();

        if (!audioManager.isConnected())
        {
            textChannel.sendMessageEmbeds(new EmbedBuilder().addField("Not connected", "I'm not currently in a voice channel", false).setColor(Color.red).build()).queue();
            return;
        }

        audioManager.closeAudioConnection();
    }

    // When a command is sent, it gets caught here and calls the Handler object to parse.
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        Guild guild = event.getGuild();
        TextChannel textChannel = event.getChannel().asTextChannel();
        Member member = event.getMember();

        List<Message.Attachment> attachments = event.getMessage().getAttachments();

        String [] arg = message.split(" ");
        assert member != null;
        if (!member.getUser().isBot())
        {
            if (arg[0].compareTo(Config.get("COMMAND_PREFIX")) == 0)
            {
                new DiscordHandler(guild, textChannel, member, arg, attachments);
            }
        }
    }

    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (Objects.requireNonNull(event.getMember()).getUser().isBot() || !devChannel.containsKey(event.getGuild().getIdLong()))
            return;

        if (devChannel.get(event.getGuild().getIdLong()) == event.getMessageIdLong())
        {
            if (event.getEmoji().getName().equals("✅"))
            {
                aws.updateTableItem("SoundByteServerList", "ServerID", event.getGuild().getId(), "Dev Channel Preference", "true");
                event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.cyan)
                            .addField("Dev channel", "Will keep the dev channel", false)
                            .build())
                        .queue();
                Bot.log(getLogType(), event.getGuild().getName() + " decided to keep the dev channel");
            }
            else if (event.getEmoji().getName().equals("❌"))
            {
                event.getChannel().asTextChannel().delete().queue();
                aws.updateTableItem("SoundByteServerList", "ServerID", event.getGuild().getId(), "Dev Channel Preference", "false");
                Bot.log(getLogType(), event.getGuild().getName() + " decided to remove the dev channel");
            }

            devChannel.remove(event.getGuild().getIdLong());
        }
    }

    // Static call to shut down the program (only the bot owner can execute the command that calls this method).
    public static void shutdown(TextChannel textChannel) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.addField(Objects.requireNonNull(textChannel.getGuild().getMemberById(Config.get("OWNER_ID"))).getEffectiveName(), "Shutting down now", true).setColor(Color.cyan);

        textChannel.sendMessageEmbeds(eb.build()).complete();

        shutdown();
    }
	
	// Closes down the logger and shuts down the program.
    private static void shutdown() {
        writeLog();
        aws.awsClose();

        jda.shutdown();

        System.exit(0);
    }

    // Static call to set the current activity in the bot profile banner.
    public static void setActivity(String activity) {
    	jda.getPresence().setActivity(Activity.playing(activity));
    }

    // Static call to get the String form of the current date-time (used mainly for logging purposes)
    public static String getDateTime() {
        return (getDate() + " <" + getTime() + ">");
    }

    // Static call to get the String form of the current date.
    public static String getDate() {
        return getLocalDate().format(DateTimeFormatter.ofPattern("y-M-d"));
    }

    // Static call to get the String form of the current time.
    public static String getTime() {
        return getLocalTime().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));
    }

    // Static call to get the current date variable.
    public static LocalDate getLocalDate() {
    	return LocalDate.now(Clock.system(ZoneId.of("America/Chicago")));
    }    

    // Static call to get the current time variable.
    public static LocalTime getLocalTime() {
    	return LocalTime.now(Clock.system(ZoneId.of("America/Chicago")));
    }

    // Main and stuff.
    public static void main(String[] args) throws InterruptedException {

        Bot bot = new Bot();

        bot.logStart();

        bot.awsInit();

        bot.loadConfig();

        bot.awsInitBucket();

        boolean isStarted = bot.startBot();

        if (!isStarted) {
            log(getLogType(), "Could not start bot");
            shutdown();
        }

        log(getLogType(), "Successfully started and connected online.");

        File downloadDir = new File("downloads");
        if (!downloadDir.exists())
        {
            log(getLogType(), "Making downloads directory");
            //noinspection ResultOfMethodCallIgnored
            downloadDir.mkdir();
        }
    }
}
