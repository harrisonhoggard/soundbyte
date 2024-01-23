package commands.util;

import bot.Bot;
import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;

import static commands.util.CommandObject.getLogType;

// Contains all the common operations that are executed by join sound related commands
public class JSHandler {

    // Method that is called when uploading a sound file to the appropriate bucket
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean uploadSound(TextChannel textChannel, List<Message.Attachment> attachments, String fileName, String bucketName, double lengthTime, int fileSize) {
        if (attachments.isEmpty())
        {
            Bot.log(getLogType(), "no sound file is attached. Cannot continue");
            textChannel.sendMessageEmbeds(new EmbedBuilder()
                    .setColor(Color.red)
                    .addField("No appropriate sound file attached", "Supported file types are: \n* .ogg\n* .mp3\n* .mp4\n* .wav\n* .wma", false)
                    .build())
                .queue();

            return false;
        }

        String address = attachments.get(0).getUrl().replaceAll("[?].+", "");
        URL url;
        File sourceFile = new File("downloads/tempSoundFile." + attachments.get(0).getFileExtension());
        try {
            url = new URL(address);
            URLConnection urlConnection = url.openConnection();

            if ((urlConnection.getContentLengthLong() / 1024) > fileSize)
            {
                Bot.log(getLogType(), "File " + sourceFile.getName() + " size is too big: " + (urlConnection.getContentLengthLong() / 1024) + " KB");
                textChannel.sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.red)
                            .addField("Failure", "The file size is too big. Be sure to send a sound file less than " + fileSize + " KB in size."
                                    + "\n\nSupported file types are: \n* .ogg\n* .mp3\n* .mp4\n* .wav\n* .wma", false)
                            .build())
                    .queue();

                return false;
            }

            FileUtils.copyURLToFile(url, sourceFile);
        } catch (IOException e) {
            Bot.log(getLogType(), "Could not download the sound file to prepare for uploading");
            return false;
        }

        File target = new File("downloads/tempSoundFile.ogg");

        // Conversion taken from documentation here: https://www.sauronsoftware.it/projects/jave/manual.php
        switch (Objects.requireNonNull(attachments.get(0).getFileExtension()))
        {
            case "mp4":
            case "wav":
            case "wma":
            case "mp3":
            {
                AudioAttributes audio = new AudioAttributes();
                audio.setCodec("vorbis");
                EncodingAttributes attrs = new EncodingAttributes();
                attrs.setFormat("ogg");
                attrs.setAudioAttributes(audio);
                Encoder encoder = new Encoder();
                try {
                    encoder.encode(sourceFile, target, attrs);
                } catch (EncoderException e) {
                    System.out.println("Could not complete:\n\t" + e);
                    //noinspection ResultOfMethodCallIgnored
                    sourceFile.delete();
                    return false;
                }

                break;
            }
            case "ogg":
            {
                target = sourceFile;
                break;
            }
            default:
            {
                Bot.log(getLogType(), "file attached was a \"." + attachments.get(0).getFileExtension() + "\" file; aborting");
                textChannel.sendMessageEmbeds(new EmbedBuilder()
                                .addField("Wrong file format", "Supported file types are: \n* .ogg\n* .mp3\n* .mp4\n* .wav\n* .wma", false).build())
                        .queue();
                //noinspection ResultOfMethodCallIgnored
                sourceFile.delete();
                return false;
            }
        }

        if (calculateDuration(target) >= lengthTime)
        {
            textChannel.sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.red)
                        .addField("Failure", "The join sound length of time was too long. Be sure to send a file less than " + lengthTime / 1000 + " seconds.", false)
                        .build())
                    .queue();
            sourceFile.delete();
            target.delete();
            return false;
        }
        else
        {
            textChannel.sendMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.green)
                            .addField("Success", "The join sound has been set", false)
                            .build())
                    .queue();
        }

        if (Bot.aws.verifyObject(bucketName, fileName))
            Bot.aws.deleteObject(bucketName, fileName);

        Bot.aws.uploadObject(bucketName, fileName, target.getPath());

        sourceFile.delete();
        target.delete();

        return true;
    }

    // Helper method determining if a file is under a specific length of time
    // Influenced by https://stackoverflow.com/a/44407355
    private static double calculateDuration(final File oggFile)  {
        int rate = -1;
        int length = -1;

        int size = (int) oggFile.length();
        byte[] t = new byte[size];

        FileInputStream stream;
        try {
            stream = new FileInputStream(oggFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            //noinspection ResultOfMethodCallIgnored
            stream.read(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int i = size-1-8-2-4; i>=0 && length<0; i--) { //4 bytes for "OggS", 2 unused bytes, 8 bytes for length
            // Looking for length (value after last "OggS")
            if (t[i]==(byte)'O' && t[i+1]==(byte)'g' && t[i+2]==(byte)'g' && t[i+3]==(byte)'S')
            {
                byte[] byteArray = new byte[]{t[i+6],t[i+7],t[i+8],t[i+9],t[i+10],t[i+11],t[i+12],t[i+13]};
                ByteBuffer bb = ByteBuffer.wrap(byteArray);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                length = bb.getInt(0);
            }
        }
        for (int i = 0; i<size-8-2-4 && rate<0; i++) {
            // Looking for rate (first value after "vorbis")
            if (t[i]==(byte)'v' && t[i+1]==(byte)'o' && t[i+2]==(byte)'r' && t[i+3]==(byte)'b' && t[i+4]==(byte)'i' && t[i+5]==(byte)'s')
            {
                byte[] byteArray = new byte[]{t[i+11],t[i+12],t[i+13],t[i+14]};
                ByteBuffer bb = ByteBuffer.wrap(byteArray);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                rate = bb.getInt(0);
            }

        }
        try {
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return (double) (length*1000) / (double) rate;
    }
}
