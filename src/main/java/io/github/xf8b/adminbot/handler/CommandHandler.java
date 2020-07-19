package io.github.xf8b.adminbot.handler;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Map;

public abstract class CommandHandler {
    private final String name;
    private final String usage;
    private final String description;
    private final Map<String, String> actions;
    private final List<String> aliases;
    private final CommandType commandType;

    public CommandHandler(String name, String usage, String description, Map<String, String> actions, List<String> aliases, CommandType commandType) {
        this.name = name;
        this.usage = usage;
        this.description = description;
        this.actions = actions;
        this.aliases = aliases;
        this.commandType = commandType;
    }

    public abstract void onCommandFired(MessageReceivedEvent event);

    public String getName() {
        return name;
    }

    public String getUsage() {
        return usage;
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

    public CommandType getCommandType() {
        return commandType;
    }

    public enum CommandType {
        ADMINISTRATION(0),
        LEVELING(1),
        OTHER(2);

        private final int index;

        CommandType(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }
}
