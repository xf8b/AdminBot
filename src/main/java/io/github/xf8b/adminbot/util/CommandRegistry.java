package io.github.xf8b.adminbot.util;

import io.github.xf8b.adminbot.handler.CommandHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Used to find command handlers when their respective commands are fired.
 *
 * @author xf8b
 */
public class CommandRegistry implements Iterable<CommandHandler> {
    private final ArrayList<CommandHandler> commandHandlers;

    public CommandRegistry() {
        commandHandlers = new ArrayList<>();
    }

    /**
     * Registers the passed in {@link CommandHandler}(s).
     *
     * @param commandHandlers The command handler(s) to be registered.
     */
    public void registerCommandHandlers(CommandHandler... commandHandlers) {
        this.commandHandlers.addAll(Arrays.asList(commandHandlers));
    }

    public int amountOfCommands() {
        return commandHandlers.size();
    }

    @Nonnull
    @Override
    public Iterator<CommandHandler> iterator() {
        return commandHandlers.iterator();
    }
}
