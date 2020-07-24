package io.github.xf8b.adminbot.listener;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.handler.CommandHandler;
import io.github.xf8b.adminbot.helper.LevelsDatabaseHelper;
import io.github.xf8b.adminbot.helper.PrefixesDatabaseHelper;
import io.github.xf8b.adminbot.util.LevelUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.SQLException;
import java.util.List;

public class MessageListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().isFromGuild()) return;
        if (event.isWebhookMessage()) return;
        if (event.getAuthor().isBot()) return;
        String guildId = event.getGuild().getId();
        String userId = event.getAuthor().getId();
        try {
            if (!PrefixesDatabaseHelper.doesGuildExistInDatabase(guildId)) {
                PrefixesDatabaseHelper.insertIntoPrefixes(guildId, AdminBot.getInstance().DEFAULT_PREFIX);
            }
            AdminBot.getInstance().prefix = PrefixesDatabaseHelper.readFromPrefixes(guildId);
            if (!LevelsDatabaseHelper.isUserInDatabase(guildId, userId)) {
                LevelsDatabaseHelper.insertIntoLevels(guildId, userId, 0, 0);
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        Message message = event.getMessage();
        String content = message.getContentRaw();
        String commandType = content.trim().split(" ")[0];
        boolean wasCommandUsed = false;
        for (CommandHandler commandHandler : AdminBot.getInstance().COMMAND_REGISTRY) {
            String name = commandHandler.getNameWithPrefix();
            List<String> aliases = commandHandler.getAliasesWithPrefixes();
            if (commandType.toLowerCase().equals(name)) {
                commandHandler.onCommandFired(event);
                wasCommandUsed = true;
            } else if (!aliases.isEmpty()) {
                for (String alias : aliases) {
                    if (commandType.toLowerCase().equals(alias)) {
                        commandHandler.onCommandFired(event);
                        wasCommandUsed = true;
                    }
                }
            }
        }
        if (!wasCommandUsed) {
            try {
                long previousXP = LevelsDatabaseHelper.getXPForUser(guildId, userId);
                long newXP = previousXP + LevelUtil.randomXp(5, 10);
                int previousLevel = LevelsDatabaseHelper.getLevelForUser(guildId, userId);
                int newLevel = LevelUtil.xpToLevels(newXP);
                if (newLevel > previousLevel) {
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + " just reached level " + newLevel + "!").queue();
                }
                LevelsDatabaseHelper.overwriteXP(guildId, userId, newXP);
                LevelsDatabaseHelper.overwriteLevel(guildId, userId, newLevel);
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
