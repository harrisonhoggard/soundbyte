package bot;

import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.*;

// Configuration file used to retrieve important information (such as admin role name, bot name, etc.).
public class Config {

    // All the necessary information to initialize the bot.
    // Do not change these! These will be retrieved in a private DynamoDB Table that you need to fill in manually!
    //      The bot will create the table, you just need to fill in the info.
    private static final String TOKEN = "TOKEN";
    private static final String COMMAND_PREFIX = "COMMAND_PREFIX";
    private static final String ADMIN_ROLE = "ADMIN_ROLE";
    private static final String ACTIVITY = "ACTIVITY";
    private static final String BOT_NAME = "BOT_NAME";
    private static final String OWNER_ID = "OWNER_ID";

    private static Map<String, String> defaults;

    private static boolean initialized;

    // The name that is printed in the logger.
    private static String getLogType() {
        return "CONFIG";
    }

    // Determines whether sufficient data was initialized to start the bot.
    public static boolean isInitialized() {
        Bot.log(getLogType(), "Config initialized?: " + initialized);
        return initialized;
    }

    // Calls the private initialization method, and checks (redundantly) whether the API token was successfully retrieved.
    public static void init() {
        initInfo();

        if (defaults.get(TOKEN) != null)
            initialized = true;
    }

    // Scans the DynamoDb table with the information, and determines whether the bot can start or not.
    private static void initInfo() {
        ScanResponse response = Bot.aws.scanItems("SoundByteInfo");

        // Sets up table if it is empty. If the table was empty, the attributes need to be filled in MANUALLY.
        // Once all the information is present in the table, restart the program and it will continue.
        if (response.items().isEmpty())
        {
            Bot.log(getLogType(), "No data present in table \"SoundByteInfo\"");

            List<String> keys = new ArrayList<>();

            keys.add("ID");
            keys.add("Value");

            setUpKey(keys, COMMAND_PREFIX);
            setUpKey(keys, ADMIN_ROLE);
            setUpKey(keys, ACTIVITY);
            setUpKey(keys, BOT_NAME);
            setUpKey(keys, OWNER_ID);

            Bot.log(getLogType(), "BE SURE TO INPUT NECESSARY INFO INTO THE \"SoundByteInfo\" TABLE");
            return;
        }

        defaults = new HashMap<>();

        // If table is missing any attribute, stop and let owner know. Otherwise, store values in Config variables
        for (Map<String, AttributeValue> item : response.items()) {
            if (item.get("Value").s().equals("null"))
            {
                Bot.log(getLogType(), "BE SURE TO INPUT NECESSARY INFO INTO THE \"SoundByteInfo\" TABLE");
                return;
            }
            defaults.put(item.get("ID").s(), item.get("Value").s());
        }

		// This is the secret's name that is stored in AWS Secrets Manager. This will need to be changed for your own secret.
        defaults.put(TOKEN, Bot.aws.getToken("DiscordRingtones"));
    }

	// Helper method for setting up the attributes in the Bot Info table.
    private static void setUpKey(List<String> keys, String keyVal1)
    {
        List<String> keyVals = new ArrayList<>();

        keyVals.add(keyVal1);
        keyVals.add("null");
        Bot.aws.addTableItem("SoundByteInfo", keys, keyVals);
    }
    
	// Public call to retrieve the stored information, except the token.
    public static String get(String key) {
        return defaults.get(key);
    }

    // Security and stuff
    public static void destroyToken() {
    	defaults.put(TOKEN, "");
    }
}
