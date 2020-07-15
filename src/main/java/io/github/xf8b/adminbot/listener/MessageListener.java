package io.github.xf8b.adminbot.listener;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.LevelsDatabaseHelper;
import io.github.xf8b.adminbot.helper.PrefixesDatabaseHelper;
import io.github.xf8b.adminbot.util.LevelUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class MessageListener extends ListenerAdapter {
    boolean hasPrefixBeenRead = false;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().isFromGuild()) return;
        if (event.getAuthor().isBot()) return;
        String guildId = event.getGuild().getId();
        String userId = event.getAuthor().getId();
        try {
            if (!hasPrefixBeenRead) {
                if (!PrefixesDatabaseHelper.doesGuildExistInDatabase(guildId)) {
                    PrefixesDatabaseHelper.insertIntoPrefixes(guildId, AdminBot.DEFAULT_PREFIX);
                }
                AdminBot.prefix = PrefixesDatabaseHelper.readFromPrefixes(guildId);
                hasPrefixBeenRead = true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        try {
            if (!LevelsDatabaseHelper.isUserInDatabase(guildId, userId)) {
                LevelsDatabaseHelper.insertIntoLevels(guildId, userId, 0, 0);
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        Message message = event.getMessage();
        String content = message.getContentRaw();
        String commandType = content.split(" ")[0];
        boolean wasCommandUsed = false;
        for (Class<?> clazz : AdminBot.commandRegistry) {
            String name = AdminBot.commandRegistry.getNameOfCommand(clazz);
            String[] aliases = AdminBot.commandRegistry.getAliasesOfCommand(clazz);
            boolean ignoreAliases = false;
            if (aliases.length == 0) ignoreAliases = true;
            if (commandType.toLowerCase().equals(name)) {
                try {
                    AdminBot.commandRegistry.getMethodOfCommand(clazz).invoke(null, event);
                    wasCommandUsed = true;
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    exception.printStackTrace();
                }
            } else if (!ignoreAliases) {
                for (String alias : aliases) {
                    if (commandType.toLowerCase().equals(alias)) {
                        try {
                            AdminBot.commandRegistry.getMethodOfCommand(clazz).invoke(null, event);
                            wasCommandUsed = true;
                        } catch (IllegalAccessException | InvocationTargetException exception) {
                            exception.printStackTrace();
                        }
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
