package bot;

import aws.AWSHandler;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import commands.util.CommandObject;
import commands.util.CommandHandler;
import events.util.EventObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
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

    private static final EventWaiter eventWaiter = new EventWaiter();

    public static Map<Guild, TextChannel> defaultChannels = new HashMap<>();

    // Starts the custom logger.
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
            Config.init();

        Bot.log(getLogType(), "Bot initialized?: " + Config.isInitialized());
        if (!Config.isInitialized())
            shutdown();
    }

    // Initializes the JDA variable and everything else required for the bot to run.
    private boolean startBot() {
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

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            Bot.log(getLogType(), "JDA could not start");
        }

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

    // Creates the required admin role for a server if it does not yet exist (only if the bot has permission to do so).
    private static void createAdminRole(Guild guild) {
        if (guild.getRolesByName(Config.get("ADMIN_ROLE"), true).isEmpty() && guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
        {
            guild.createRole()
                    .setName(Config.get("ADMIN_ROLE"))
                    .setColor(Color.red)
                    .setMentionable(true)
                    .complete();

            log(getLogType(), "Created " + Config.get("ADMIN_ROLE") + " role in guild " + guild.getId());

            defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.green)
                            .addField("Role created", "Created " + guild.getRolesByName(Config.get("ADMIN_ROLE"), true).get(0).getAsMention() +
                                    " role and assigned it to the server owner \"" + Objects.requireNonNull(guild.getOwner()).getAsMention() +
                                    "\". Be sure you give this only to people you trust.", false)
                            .build())
                    .queue();

            guild.addRoleToMember(Objects.requireNonNull(guild.getOwner()), guild.getRolesByName(Config.get("ADMIN_ROLE"), true).get(0))
                    .complete();
            log(getLogType(), "Gave the guild owner the " + Config.get("ADMIN_ROLE") + " role");
        }
        else if (!guild.getRolesByName(Config.get("ADMIN_ROLE"), true).isEmpty())
        {
            defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.cyan)
                            .addField(Config.get("ADMIN_ROLE"), "Be sure to give this role only to people you trust.", false)
                            .build())
                    .queue();

            log(getLogType(), guild.getId() + " created the " + Config.get("ADMIN_ROLE") + " role.");
        }
    }

    // Initializes everything required for each Discord server in which the bot is present.
    public static void guildInit(Guild guild) {

        List<String> keys = new ArrayList<>();
        List<String> keyVals = new ArrayList<>();

        keys.add("ServerID");
        keyVals.add(guild.getId());

        // Handles setting up the default channel that the bot can send messages
        boolean canTalk = false;
        TextChannel visibleChannel = null;
        // TRIGGER WARNING: Nesting!
        if (!aws.getItem("SoundByteServerList", "ServerID", guild.getId()).isEmpty())
        {
            if (aws.getItem("SoundByteServerList", "ServerID", guild.getId()).get("Default Channel") == null)
            {
                for (TextChannel channel : guild.getTextChannels())
                {
                    if (channel.canTalk())
                    {
                        visibleChannel = channel;
                        aws.updateTableItem("SoundByteServerList", "ServerID", guild.getId(), "Default Channel", visibleChannel.getId());
                        break;
                    }
                }
            }
            else
                visibleChannel = guild.getTextChannelById(aws.getItem("SoundByteServerList", "ServerID", guild.getId()).get("Default Channel").s());
        }

        else {
            for (TextChannel channel : guild.getTextChannels()) {
                if (channel.canTalk())
                {
                    canTalk = true;
                    visibleChannel = channel;
                    break;
                }
            }

            if (!canTalk) {
                try {
                    Objects.requireNonNull(guild.getOwner()).getUser().openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(new EmbedBuilder()
                                    .setColor(Color.red)
                                    .addField("**IMPORTANT** -- Insufficient permissions", "I don't have permission to view any channels." +
                                            " I will not be able to function properly or set up anything on my end. Please make sure that both" +
                                            " a text and voice channel are viewable for me, and react to this message with any emoji when this is done.", false)
                                    .build()))
                            .complete();

                    log(getLogType(), guild.getId() + ": cannot view channels. Sent dm to \"" + guild.getOwner().getId() + "\"");

                    eventWaiter.waitForEvent(
                            MessageReactionAddEvent.class,
                            e -> !e.isFromGuild() && !e.retrieveUser().complete().isBot(),
                            e -> {
                                log(getLogType(), guild.getId() + " owner reacted");
                                guildInit(guild);
                            }
                    );
                } catch (InsufficientPermissionException | ErrorResponseException e) {
                    log(getLogType(), "Could not private message " + Objects.requireNonNull(guild.getOwner()).getId() + " from guild " + guild.getId());
                }
                return;
            }
        }

        if (visibleChannel != null)
        {
            defaultChannels.put(guild, visibleChannel);
            log(getLogType(), guild.getId() + ": set default channel to " + defaultChannels.get(guild).getId());
        }

        if (aws.getItem("SoundByteServerList", "ServerID", guild.getId()).isEmpty())
        {
            defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.cyan)
                            .addField("Channel chosen", "I will send messages in here, but you can change this by using **\"" + Config.get("COMMAND_PREFIX") + " channel\"**.", false)
                            .build())
                    .queue(m -> log(getLogType(), "Notified guild " + guild.getId() + " which channel will be used."));
        }

        // Handles the absence of an Admin role in a server
        if (guild.getRolesByName(Config.get("ADMIN_ROLE"), true).isEmpty())
        {
            if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
                createAdminRole(guild);
            else if (aws.getItem("SoundByteServerList", "ServerID", guild.getId()).isEmpty())
            {
                log(getLogType(), guild.getId() + ": asked guild to either create role or let me do it for them");
                defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("No " + Config.get("ADMIN_ROLE") + " role is present", "Some commands require a role named \"" +
                                    Config.get("ADMIN_ROLE") + "\" to execute. Either create one, or give me permission to manage roles and I'll do it for you.", false)
                            .build())
                        .queue(message -> eventWaiter.waitForEvent(
                                Event.class,
                                e -> guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES) || !guild.getRolesByName(Config.get("ADMIN_ROLE"), true).isEmpty(),
                                e -> {
                                    if (e.toString().compareTo("GuildMemberRoleAddEvent") == 0 || e.toString().compareTo("RoleUpdateNameEvent[name](new role -> " + Config.get("ADMIN_ROLE") + ")") == 0)
                                        createAdminRole(guild);
                                }
                        ));
            }
        }
        else if (aws.getItem("SoundByteServerList", "ServerID", guild.getId()).isEmpty())
        {
            defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.yellow)
                        .addField("ADMIN", "Some commands require the " + guild.getRolesByName(Config.get("ADMIN_ROLE"), true).get(0).getAsMention() +
                                " role to use. Make sure only people you trust have this role.", false)
                        .build())
                    .queue();
        }

        // Anyone know how else to do this besides the cursed way I've done it???
        if (aws.getItem("SoundByteServerList", "ServerID", guild.getId()).isEmpty())
        {
            aws.addTableItem("SoundByteServerList", keys, keyVals);
            aws.updateTableItem("SoundByteServerList", "ServerID", guild.getId(), "Default Channel", defaultChannels.get(guild).getId());

            defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.yellow)
                            .addField("**!!! IMPORTANT !!! **", "Give me about 30 seconds to setup everything before using commands. " +
                                    "I'll send some information here in the meantime, and let you know when I'm ready.", false)
                            .build())
                    .queue();

            defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .setTitle("About me")
                        .setDescription("I am capable of playing short sounds over voice chat whenever someone connects to a channel. " +
                                "There are default sounds that apply to everyone, but you can customize them if you want. To get started, just type in **\"" + Config.get("COMMAND_PREFIX") +
                                " help\"** for more information." +
                                "\n\nIn order to upload a sound, just type in **\"" + Config.get("COMMAND_PREFIX") + " add\"**, and then attach a supported sound file in the same message." +
                                "\nYou can also remove with **\"" + Config.get("COMMAND_PREFIX") + " remove\"**, " +
                                "and you can add other people's sounds if you have the **\"" + Config.get("ADMIN_ROLE") + "\"** role by entering **\"" +
                                Config.get("COMMAND_PREFIX") + " add @someone\"**. You can also remove sounds the same way." +
                                "\n\nYou can also remove with **\"" + Config.get("COMMAND_PREFIX") + " remove\"**, " +
                                "and you can add other people's sounds if you have the **\"" + Config.get("ADMIN_ROLE") + "\"** role by entering **\"" +
                                Config.get("COMMAND_PREFIX") + " add @someone\"**. You can also remove sounds the same way.")
                        .build())
                    .queue();
            defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .addField("Contact", "If you have any questions, or need to report any problems, feel free to contact me at " +
                                "**\"crispycrusaderdev@gmail.com\"**", false)
                        .build())
                    .queue();
        }

        // Creates each guild's join sound bucket if it does not yet exist.
        String bucketName = guild.getId() + "-joinsounds";
        aws.initS3Bucket(bucketName);
        helpUpload(bucketName, "default.ogg", "/audioFiles/default.ogg");
        helpUpload(bucketName, "rare.ogg", "/audioFiles/rare.ogg");
        helpUpload(bucketName, "ultraRare.ogg", "/audioFiles/ultraRare.ogg");

		// Creates each guild's table for storing ultrarare information (takes about 30 seconds to finish if it doesn't exist)
        String tableName = guild.getId() + "-ultrarare";
        AttributeDefinition tableAttributes = AttributeDefinition.builder()
                .attributeName("MemberID")
                .attributeType("S")
                .build();
        boolean result = aws.verifyDynamoTable(tableName);

        if (!result)
        {
            log(getLogType(), tableName + " table does not exist, creating now...");
            aws.createDynamoTable(tableName, tableAttributes);
            log(getLogType(), tableName + " table was created");

            defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.green)
                        .addField("Initialization completed", "You can now use me. Type in \"" + Config.get("COMMAND_PREFIX") + " help\" to get started.", false)
                        .build())
                    .queue(message -> log(getLogType(), guild.getId() + ": Initialization complete"));
        }

        log(getLogType(), guild.getId() + ": initialized guild with " + guild.getMembers().size() + " members.");

        keys.clear();
        keyVals.clear();
    }

    // Static call for joining a voice channel.
    public static void joinVc(Guild guild, MessageChannel channel, VoiceChannel voiceChannel) {
        AudioManager audioManager = guild.getAudioManager();

        if (audioManager.isConnected() && Objects.requireNonNull(audioManager.getConnectedChannel()).asVoiceChannel().equals(voiceChannel))
        {
            channel.sendMessageEmbeds(new EmbedBuilder().addField("", "Already connected to voice channel \"" + voiceChannel.getName() + "\"", false).setColor(Color.yellow).build()).queue();
            return;
        }

        try {
            audioManager.openAudioConnection(voiceChannel);
        } catch(Exception e) {
            channel.sendMessageEmbeds(new EmbedBuilder().addField("", "Could not join voice channel \"" + voiceChannel.getName(), false).setColor(Color.red).build()).queue();
        }
    }

    // Static call for leaving a voice channel.
    public static void leaveVc(Guild guild, MessageChannel channel) {
        AudioManager audioManager = guild.getAudioManager();

        if (!audioManager.isConnected())
        {
            channel.sendMessageEmbeds(new EmbedBuilder().addField("Not connected", "I'm not currently in a voice channel", false).setColor(Color.red).build()).queue();
            return;
        }

        audioManager.closeAudioConnection();
    }

    // When a command is sent, it gets caught here and calls the Handler object to parse.
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getChannelType().equals(ChannelType.TEXT) && !event.getChannelType().equals(ChannelType.VOICE))
            return;

        Guild guild = event.getGuild();
        MessageChannel channel = event.getChannel();
        String message = event.getMessage().getContentRaw();

        // Some bot profiles are null(?) accounts, and it results in a NullPointerException when they send messages. This fixes it.
        if (event.getMember() == null)
            return;

        Member member = event.getMember();

        List<Message.Attachment> attachments = event.getMessage().getAttachments();

        String [] arg = message.split(" ");

        if (!member.getUser().isBot())
        {
            if (arg[0].compareTo(Config.get("COMMAND_PREFIX")) == 0)
            {
                new CommandHandler(guild, channel, member, arg, attachments);
            }
        }
    }

    // Static call to shut down the program (only the bot owner can execute the command that calls this method).
    public static void shutdown(MessageChannel channel, Member member) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.addField(member.getEffectiveName(), "Shutting down now", true).setColor(Color.cyan);

        channel.sendMessageEmbeds(eb.build()).complete();

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
        return getLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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
    public static void main(String[] args) {

        //Thread.UncaughtExceptionHandler globalExceptionHandler = new UncaughtException();
        //Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
        //Thread.currentThread().setUncaughtExceptionHandler(globalExceptionHandler);

        Bot bot = new Bot();

        bot.logStart();

        bot.awsInit();

        bot.loadConfig();

        bot.awsInitBucket();

        boolean isStarted = bot.startBot();

        if (!isStarted)
        {
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
