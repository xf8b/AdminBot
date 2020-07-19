package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.helper.LevelsDatabaseHelper;
import io.github.xf8b.adminbot.util.LevelUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;

public class LevelCommandHandler extends CommandHandler {
    public LevelCommandHandler() {
        super(
                "${prefix}level",
                "${prefix}level",
                "Shows your XP and level.",
                ImmutableMap.of(),
                ImmutableList.of("${prefix}rank"),
                CommandType.LEVELING
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
            MessageChannel channel = event.getChannel();
            String guildId = event.getGuild().getId();
            String userId = event.getAuthor().getId();
            long xp = LevelsDatabaseHelper.getXPForUser(guildId, userId);
            int level = LevelsDatabaseHelper.getLevelForUser(guildId, userId);
            MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Level For " + event.getAuthor().getName())
                    .addField("Level", String.valueOf(level), true)
                    .addField("XP", String.valueOf(xp), true)
                    .addField("XP For Next Level", LevelUtil.remainingXp(xp) + "/" + LevelUtil.xpToNextLevel(level), true)
                    .setColor(Color.BLUE)
                    .build();
            channel.sendMessage(embed).queue();
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
