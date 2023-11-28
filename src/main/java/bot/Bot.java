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
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.Event;
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
import java.util.concurrent.TimeUnit;

// Main method contained here. Responsible for initialization of everything, as well as storing all the helper methods that are needed throughout the entire program.
public class Bot extends ListenerAdapter{

	public static JDA jda;

    private static Logger logger;
    public static AWSHandler aws;

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

    // Creates the required admin role if it does not yet exist.
    private static void createAdminRole(Guild guild) {
        if (guild.getRolesByName(Config.get("ADMIN_ROLE"), true).isEmpty() && guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
        {
            guild.createRole()
                        .setName(Config.get("ADMIN_ROLE"))
                        .setColor(Color.red)
                        .setMentionable(true)
                        .complete();

            log(getLogType(), "Created " + Config.get("ADMIN_ROLE") + " role in guild " + guild.getName());

            Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
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
            Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .addField(Config.get("ADMIN_ROLE"), "Be sure to give this role only to people you trust.", false)
                        .build())
                    .queue();

            log(getLogType(), guild.getName() + " created the " + Config.get("ADMIN_ROLE") + " role.");
        }
    }

    // Initializes everything required for each Discord server in which the bot is present.
    public static void guildInit(Guild guild) {

        List<String> keys = new ArrayList<>();
        List<String> keyVals = new ArrayList<>();

        keys.add("ServerID");
        keyVals.add(guild.getId());

        if (guild.getRolesByName(Config.get("ADMIN_ROLE"), true).isEmpty())
        {
            if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
                createAdminRole(guild);
            else
            {
                Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("No " + Config.get("ADMIN_ROLE") + " role is present", "Some commands require a role named \"" +
                                    Config.get("ADMIN_ROLE") + "\" to execute. Either create one, or give me permission to manage roles and I'll do it for you.", false)
                            .build())
                        .queue(message -> eventWaiter.waitForEvent(
                                Event.class,
                                e -> guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES) || !guild.getRolesByName(Config.get("ADMIN_ROLE"), true).isEmpty(),
                                e -> {
                                    if (e.toString().compareTo("GuildMemberRoleAddEvent") == 0|| e.toString().compareTo("RoleUpdateNameEvent[name](new role -> " + Config.get("ADMIN_ROLE") + ")") == 0)
                                        createAdminRole(guild);
                                }
                        ));
            }
        }
        else if (aws.getItem("SoundByteServerList", "ServerID", guild.getId()).isEmpty())
        {
            Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.yellow)
                        .addField("ADMIN", "Some commands require the " + guild.getRolesByName(Config.get("ADMIN_ROLE"), true).get(0).getAsMention() +
                                " to use. Make sure only people you trust have this role.", false)
                        .build())
                    .queue();
        }

        // Anyone know how else to do this besides the cursed way I've done it???
        if (aws.getItem("SoundByteServerList", "ServerID", guild.getId()).isEmpty())
        {
            aws.addTableItem("SoundByteServerList", keys, keyVals);

            Objects.requireNonNull(guild.getDefaultChannel()).asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                .setColor(Color.cyan)
                .addField("**!!! IMPORTANT !!! **", "Give me about 30 seconds to setup everything before using commands. " +
                        "I'll send some information here in the meantime, and let you know when I'm ready.", false)
                .build())
            .queue(message -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
                    guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.cyan)
                        .addField(Config.get("BOT_NAME"), "I am capable of playing short sounds over voice chat whenever someone connects to a channel. " +
                                "There are default sounds that apply to everyone, but you can customize them if you want. To get started, just type in **\"" + Config.get("COMMAND_PREFIX") +
                                " help\"** for more information.", false)
                        .build())
                    .queue(message2 -> {
                        try {
                            TimeUnit.SECONDS.sleep(2);guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                                .setColor(Color.cyan)
                                .addField("Commands", "In order to upload a sound, just type in **\"" + Config.get("COMMAND_PREFIX") +
                                        " add\"**, and then attach a sound file with the extension **\".ogg\"** in the same message. " +
                                        "If you're not sure how to convert a sound file, just click on this link to learn how to use Audacity to do this. " +
                                        "https://www.cedarville.edu/insights/computer-help/post/convert-audio-files.", false)
                                .build())
                            .queue(message3 -> {
                                try {
                                    TimeUnit.SECONDS.sleep(2);
                                    guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                                        .setColor(Color.cyan)
                                        .addField("Commands (cont.)", "You can also remove with **\"" + Config.get("COMMAND_PREFIX") + " remove\"**, " +
                                                "and you can add other people's sounds if you have the **\"" + Config.get("ADMIN_ROLE") + "\"** role by entering **\"" +
                                                Config.get("COMMAND_PREFIX") + " add @someone\"**. You can also remove sounds the same way.", false)
                                        .build())
                                    .queue(message4 -> {
                                        try {
                                            TimeUnit.SECONDS.sleep(2);guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                                                .setColor(Color.cyan)
                                                .addField("Rare and Ultra-rare", "There are two other tiers of sounds: a rare, and an ultra-rare. " +
                                                        "These are special sounds that play randomly, and can also be customized for your server. Just use **\"" +
                                                        Config.get("COMMAND_PREFIX") + " rare\"** and **\"" + Config.get("COMMAND_PREFIX") + " ultra\"** with an attached .ogg sound file to change them.", false)
                                                .build())
                                            .queue(message5 -> {
                                                try {
                                                    TimeUnit.SECONDS.sleep(2);guild.getDefaultChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                                                        .setColor(Color.cyan)
                                                        .addField("Contact", "If you have any questions, or need to report any problems, feel free to contact me at " +
                                                                "**\"crispycrusaderdev@gmail.com\"**", false)
                                                        .build())
                                                    .queue(message6 -> log(getLogType(), guild.getName() + ": Sent tutorial"));
                                                } catch (InterruptedException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            });
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

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
                    .queue(message -> log(getLogType(), guild.getName() + ": Initialization complete"));
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
