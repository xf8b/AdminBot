package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.helper.LevelsDatabaseHelper;
import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@CommandHandler(
        name = "${prefix}leaderboard",
        usage = "${prefix}leaderboard",
        description = "Brings up the leaderboard for the guild.",
        aliases = {"${prefix}top"}
)
public class LeaderboardCommandHandler {
    public static void onLeaderboardCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
        MessageChannel channel = event.getChannel();
        Guild guild = event.getGuild();
        String guildId = guild.getId();
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Leaderboard For `" + guild.getName() + "`")
                .setColor(Color.BLUE);
        Map<String, Long> xps = LevelsDatabaseHelper.getAllXPForGuild(guildId);
        ArrayList<Long> xpsSorted = new ArrayList<>();
        xps.forEach((key, value) -> xpsSorted.add(value));
        Collections.sort(xpsSorted);
        List<Long> top = xpsSorted.subList(xpsSorted.size() - Math.min(xpsSorted.size(), 5), xpsSorted.size());
        Collections.reverse(top);
        String[] xpsSortedFormatted = {""};
        int[] amountOfMembersCounted = {0};
        top.forEach(xp -> xps.forEach((key, value) -> {
            if (value.equals(xp)) {
                guild.retrieveMemberById(key).queue(member -> {
                    int numberOnLeaderboard = xpsSorted.indexOf(xp) + 1;
                    int level = 0;
                    try {
                        level = LevelsDatabaseHelper.getLevelForUser(guildId, key);
                    } catch (ClassNotFoundException | SQLException e) {
                        e.printStackTrace();
                    }
                    xpsSortedFormatted[0] = xpsSortedFormatted[0].concat("#" + numberOnLeaderboard + " " + member.getAsMention() + " XP: `" + xp + "`, Level: `" + level + "`\n");
                    amountOfMembersCounted[0]++;
                    if (amountOfMembersCounted[0] == Math.min(xpsSorted.size(), 5)) {
                        embedBuilder.setDescription(xpsSortedFormatted[0].replaceAll("[\n]*$", ""));
                        channel.sendMessage(embedBuilder.build()).queue();
                    }
                });
            }
        }));
    }
}
