package events;

import bot.Bot;
import events.util.EventObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Random;

// Very small chance to call someone out for typing in Discord
public class UserTyping extends EventObject {
    private Guild guild;

    @Override
    public String getName() {
        return "UserTyping";
    }

    @Override
    public String getAction() {
        return "got called out for typing";
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    public void onUserTyping(@NotNull UserTypingEvent event) {
    	
        guild = event.getGuild();

        Random rand = new Random();
        int randomInt = rand.nextInt(1000);
        
        if (randomInt < 1) {
			try {
        	    event.getChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                	        .setColor(Color.cyan)
                    	    .addField("OH BOY!!!", event.getUser().getAsMention() + " has been typing something!!!", false)
                        	.build())
        	            .queue();
        	} catch (InsufficientPermissionException e) {
        		Bot.defaultChannels.get(guild).sendMessageEmbeds(new EmbedBuilder()
                	        .setColor(Color.cyan)
                    	    .addField("OH BOY!!!", event.getUser().getAsMention() + " has been typing something!!!", false)
                        	.build())
        	            .queue();
        	}

            devMessage(getName(), event.getUser().getEffectiveName() + " " + getAction(), guild);
        }

    }
}
