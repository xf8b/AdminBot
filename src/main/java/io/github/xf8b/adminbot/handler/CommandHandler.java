package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.AdminBot;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class CommandHandler {
    private final String name;
    private final String usage;
    private final String description;
    private final Map<String, String> actions;
    private final List<String> aliases;
    private final CommandType commandType;
    private final int levelRequired;

    public CommandHandler(String name, String usage, String description, Map<String, String> actions, List<String> aliases, CommandType commandType, int levelRequired) {
        this.name = name;
        this.usage = usage;
        this.description = description;
        this.actions = actions;
        this.aliases = aliases;
        this.commandType = commandType;
        this.levelRequired = levelRequired;
    }

    public abstract void onCommandFired(MessageReceivedEvent event);

    public String getName() {
        return name;
    }

    public String getNameWithPrefix() {
        return name.replace("${prefix}", AdminBot.getInstance().prefix);
    }

    public String getUsage() {
        return usage;
    }

    public String getUsageWithPrefix() {
        return usage.replace("${prefix}", AdminBot.getInstance().prefix);
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getActions() {
        return actions;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<String> getAliasesWithPrefixes() {
        List<String> aliasesWithPrefixes = new ArrayList<>();
        for (String string : aliases) {
            aliasesWithPrefixes.add(string.replace("${prefix}", AdminBot.getInstance().prefix));
        }
        return aliasesWithPrefixes;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public int getLevelRequired() {
        return levelRequired;
    }

    public enum CommandType {
        ADMINISTRATION("Commands related with administration."),
        LEVELING("Commands related with leveling."),
        OTHER("Other commands which do not fit in any of the above categories.");

        private final String description;

        CommandType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
