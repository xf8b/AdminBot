package io.github.xf8b.adminbot.util;

import io.github.xf8b.adminbot.AdminBot;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Used to find command handlers when their respective commands are fired.
 *
 * @author xf8b
 */
public class CommandRegistry implements Iterable<Class<?>> {
    private final ArrayList<Class<?>> commandHandlers;
    private final Map<Class<?>, String> names;
    private final Map<Class<?>, String> descriptions;
    private final Map<Class<?>, String> usages;
    private final Map<Class<?>, Method> methods;
    private final Map<Class<?>, String> actions;
    private final Map<Class<?>, String[]> aliases;
    public boolean isUpdating = false;

    public CommandRegistry() {
        commandHandlers = new ArrayList<>();
        names = new HashMap<>();
        descriptions = new HashMap<>();
        usages = new HashMap<>();
        methods = new HashMap<>();
        actions = new HashMap<>();
        aliases = new HashMap<>();
    }

    public final void registerClass(Class<?> clazz) throws NoSuchMethodException {
        commandHandlers.addAll(Collections.singleton(clazz));
        names.put(clazz, clazz.getAnnotation(CommandHandler.class).name().replace("${prefix}", AdminBot.prefix));
        descriptions.put(clazz, clazz.getAnnotation(CommandHandler.class).description());
        usages.put(clazz, clazz.getAnnotation(CommandHandler.class).usage().replace("${prefix}", AdminBot.prefix));
        if (names.get(clazz).replace(AdminBot.prefix, "").equals("removewarn")) {
            methods.put(clazz, clazz.getMethod("onRemoveWarnCommand", MessageReceivedEvent.class));
        } else {
            methods.put(clazz, clazz.getMethod("on" + WordUtils.capitalizeFully(names.get(clazz).replace(AdminBot.prefix, "")) + "Command", MessageReceivedEvent.class));
        }
        actions.put(clazz, clazz.getAnnotation(CommandHandler.class).actions().replace("${prefix}", AdminBot.prefix));
        String[] aliasesFromClass = clazz.getAnnotation(CommandHandler.class).aliases();
        ArrayList<String> tempAliases = new ArrayList<>();
        for (String string : aliasesFromClass) {
            tempAliases.add(string.replace("${prefix}", AdminBot.prefix));
        }
        String[] aliasesParsed = {};
        aliasesParsed = tempAliases.toArray(aliasesParsed);
        aliases.put(clazz, aliasesParsed);
    }

    public String getNameOfCommand(Class<?> clazz) {
        return names.get(clazz);
    }

    public String getDescriptionOfCommand(Class<?> clazz) {
        return descriptions.get(clazz);
    }

    public String getUsageOfCommand(Class<?> clazz) {
        return usages.get(clazz);
    }

    public Method getMethodOfCommand(Class<?> clazz) {
        return methods.get(clazz);
    }

    public String getActionsOfCommand(Class<?> clazz) {
        return actions.get(clazz);
    }

    public String[] getAliasesOfCommand(Class<?> clazz) {
        return aliases.get(clazz);
    }

    public int amountOfCommands() {
        return commandHandlers.size();
    }

    public void updatePrefix() {
        isUpdating = true;
        commandHandlers.clear();
        names.clear();
        descriptions.clear();
        usages.clear();
        methods.clear();
        actions.clear();
        aliases.clear();
        RegistryUtil.getAllCommandHandlers().forEach(clazz -> {
            try {
                registerClass(clazz);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
        isUpdating = false;
    }

    @NotNull
    @Override
    public Iterator<Class<?>> iterator() {
        return commandHandlers.iterator();
    }
}
