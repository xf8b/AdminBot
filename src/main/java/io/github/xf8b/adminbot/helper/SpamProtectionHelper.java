package io.github.xf8b.adminbot.helper;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.Range;

import java.awt.*;
import java.sql.SQLException;

/**
 * A class used to check if a user is spamming
 *
 * @author xf8b
 * @deprecated as it's broken.
 */
@Deprecated
public class SpamProtectionHelper {
    @Deprecated
    public static boolean checkForSpam(MessageReceivedEvent event) {
        String guildId = event.getGuild().getId();
        String userId = event.getAuthor().getId();
        boolean[] wasSpamming = {false};
        int[] amountOfMessagesWithSameSender = {0};
        event.getChannel().getHistory().retrievePast(5).queue(messages -> {
            int second;
            int previousSecond;
            User author;
            User previousAuthor;
            for (int i = 0; i < messages.size(); i++) {
                author = messages.get(i).getAuthor();
                previousAuthor = (i == 0 ? author : messages.get(i - 1).getAuthor());
                second = messages.get(i).getTimeCreated().getSecond();
                previousSecond = (i == 0 ? second : messages.get(i - 1).getTimeCreated().getSecond());
                if (author.equals(previousAuthor)) {
                    for (int j = 1; j < 6; j++) {
                        if (Range.between(second, previousSecond).contains(second - j)) {
                            amountOfMessagesWithSameSender[0]++;
                        }
                    }
                }
            }
        });
        if (amountOfMessagesWithSameSender[0] >= 5) {
            event.getGuild().retrieveMemberById(userId).queue(member -> {
                if (member == null) {
                    throw new IllegalStateException("Member is null!");
                }
            });
            try {
                if (WarnsDatabaseHelper.doesUserHaveWarn(guildId, userId, "SPAM")) {
                    String warnId = String.valueOf(Integer.parseInt(WarnsDatabaseHelper.getAllWarnsForUser(guildId, userId).get("SPAM").iterator().next()) + 1);
                    WarnsDatabaseHelper.insertIntoWarns(guildId, userId, warnId, "SPAM");
                } else {
                    WarnsDatabaseHelper.insertIntoWarns(guildId, userId, String.valueOf(0), "SPAM");
                }
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
            event.getGuild().retrieveMemberById(userId).queue(member -> member.getUser().openPrivateChannel().queue(privateChannel -> {
                MessageEmbed embed = new EmbedBuilder()
                        .setTitle("You were warned!")
                        .addField("Server", event.getGuild().getName(), false)
                        .addField("Reason", "SPAM", false)
                        .setColor(Color.RED)
                        .build();
                privateChannel.sendMessage(embed).queue();
            }));
            wasSpamming[0] = true;
        }
        return wasSpamming[0];
    }
}
