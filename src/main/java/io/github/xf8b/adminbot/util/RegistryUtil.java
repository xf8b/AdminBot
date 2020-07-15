package io.github.xf8b.adminbot.util;

import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Set;

public class RegistryUtil {
    /**
     * Used to find all the command handlers for {@link CommandRegistry}.
     *
     * @return All of the command handlers.
     */
    public static ArrayList<Class<?>> getAllCommandHandlers() {
        Reflections reflections = new Reflections("io.github.xf8b.adminbot");

        Set<Class<?>> commandHandlerClasses = reflections.getTypesAnnotatedWith(CommandHandler.class);

        return new ArrayList<>(commandHandlerClasses);
    }
}
