package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.helper.LevelsDatabaseHelper;
import io.github.xf8b.adminbot.util.CommandHandler;
import io.github.xf8b.adminbot.util.LevelUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;

@CommandHandler(
        name = "${prefix}level",
        usage = "${prefix}level",
        description = "Gets your level.",
        aliases = {"${prefix}rank"}
)
public class LevelCommandHandler {
    public static void onLevelCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
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
    }
}
