/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.adminbot.util;

import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Used to find command handlers when their respective commands are fired.
 *
 * @author xf8b
 */
@Getter
@Slf4j
public class CommandRegistry extends AbstractList<AbstractCommandHandler> {
    private final List<AbstractCommandHandler> commandHandlers;
    private boolean locked = false;

    public CommandRegistry() {
        commandHandlers = new LinkedList<>();
    }

    /**
     * Registers the passed in {@link AbstractCommandHandler}(s).
     *
     * @param abstractCommandHandlers The command handler(s) to be registered.
     */
    public void registerCommandHandlers(AbstractCommandHandler... abstractCommandHandlers) {
        if (locked) throw new UnsupportedOperationException("Registry is currently locked!");
        this.commandHandlers.addAll(Arrays.asList(abstractCommandHandlers));
    }

    public void slurpCommandHandlers(String packagePrefix) {
        Reflections reflections = new Reflections(packagePrefix, new SubTypesScanner());
        reflections.getSubTypesOf(AbstractCommandHandler.class).forEach(clazz -> {
            try {
                registerCommandHandlers(clazz.getConstructor().newInstance());
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException exception) {
                LOGGER.error("An error happened while trying to slurp command handlers!", exception);
            }
        });
        locked = true;
    }

    @Override
    public AbstractCommandHandler get(int index) {
        return commandHandlers.get(index);
    }

    @Nonnull
    @Override
    public Iterator<AbstractCommandHandler> iterator() {
        return commandHandlers.iterator();
    }

    @Override
    public int size() {
        return commandHandlers.size();
    }

    public AbstractCommandHandler getCommandHandler(Class<? extends AbstractCommandHandler> clazz) {
        return commandHandlers.stream()
                .filter(abstractCommandHandler -> abstractCommandHandler.getClass() == clazz)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No command handler matches the class inputted!"));
    }

    public List<AbstractCommandHandler> getCommandHandlersWithCommandType(AbstractCommandHandler.CommandType commandType) {
        return new LinkedList<>(commandHandlers.stream()
                .filter(abstractCommandHandler -> abstractCommandHandler.getCommandType() == commandType)
                .collect(Collectors.toUnmodifiableList()));
    }
}
