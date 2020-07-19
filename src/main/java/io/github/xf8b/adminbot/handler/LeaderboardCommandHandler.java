package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.helper.LevelsDatabaseHelper;
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

public class LeaderboardCommandHandler extends CommandHandler {
    public LeaderboardCommandHandler() {
        super(
                "${prefix}leaderboard",
                "${prefix}leaderboard",
                "Shows the top five people who have the most XP in the guild.",
                ImmutableMap.of(),
                ImmutableList.of("top"),
                CommandType.LEVELING
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
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
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
